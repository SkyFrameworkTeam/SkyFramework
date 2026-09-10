package com.skyframework.islandcore.player.rescue;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.teleport.SafeLandingChecker;
import com.skyframework.islandcore.teleport.SafeLocationFinder;
import com.skyframework.islandcore.teleport.TeleportBackend;
import com.skyframework.islandcore.teleport.VanillaTeleportBackend;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;

// An emergency rescue teleport for a player who falls out of the world over islandcore:islands (a
// flat void dimension — see data/islandcore/dimension/islands.json — so falling off the edge of an
// unbuilt plot is the normal way to end up here, not a rare edge case). Deliberately no
// countdown/cooldown like /island home has: this is a safety net, not an action the player chose.
//
// Two independent detection paths, both funneling into the same rescue() below:
//  - isDamageAllowed, hooked to ServerLivingEntityEvents.ALLOW_DAMAGE (IslandCoreMod's third,
//    independent listener on that event) — fires on OUT_OF_WORLD damage, the normal Survival case.
//  - tickAll, hooked to ServerTickEvents.END_SERVER_TICK — a periodic Y-position check, needed
//    because a Creative-mode player is invulnerable to OUT_OF_WORLD damage (LivingEntity#damage's
//    own invulnerability short-circuit runs before ALLOW_DAMAGE ever fires for them), so
//    isDamageAllowed never runs for a Creative player and they would otherwise fall forever until
//    switching to Survival. Confirmed by reading LivingEntity's damage/invulnerability handling —
//    this isn't a hypothetical, Creative genuinely never reaches the event this class used to rely
//    on exclusively.
public final class VoidRescueListener {

	private static final RegistryKey<World> ISLANDS_DIMENSION =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

	// Matches IslandRegistryImpl's own MIN_Y (the islandcore:islands dimension's configured build
	// floor) plus a small margin: a player this far below the floor has unambiguously fallen out,
	// whether or not they've ever taken (or could take) OUT_OF_WORLD damage for it.
	private static final int MIN_Y = -64;
	private static final int FALL_RESCUE_MARGIN = 16;
	private static final int FALL_RESCUE_THRESHOLD_Y = MIN_Y + FALL_RESCUE_MARGIN;

	private static final TeleportBackend BACKEND = new VanillaTeleportBackend();

	private VoidRescueListener() {
	}

	public static boolean isDamageAllowed(LivingEntity victim, DamageSource source) {
		if (!victim.getWorld().getRegistryKey().equals(ISLANDS_DIMENSION) || !IslandCoreMod.VOID_RESCUE_CONFIG.isEnabled()) {
			return true;
		}

		if (!(victim instanceof ServerPlayerEntity player) || !source.isOf(DamageTypes.OUT_OF_WORLD)) {
			return true;
		}

		return !rescue(player);
	}

	// Independent of any damage event — see class javadoc for why this exists. Iterates online
	// players instead of hooking a per-entity event: cheap (typical server player counts), and
	// mirrors the ServerTickEvents.END_SERVER_TICK + tickAll() pattern already used by
	// TeleportManagerImpl/IslandDeletionServiceImpl/VanillaResetService.
	public static void tickAll(MinecraftServer server) {
		if (!IslandCoreMod.VOID_RESCUE_CONFIG.isEnabled()) {
			return;
		}

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (!player.getWorld().getRegistryKey().equals(ISLANDS_DIMENSION)) {
				continue;
			}
			if (player.getY() > FALL_RESCUE_THRESHOLD_Y) {
				continue;
			}
			rescue(player);
		}
	}

	// Shared by both detection paths above: resolves the player's own island (or Spawn if they
	// don't have one) and its home location, validating/self-correcting it exactly the same way
	// regardless of which path triggered the rescue. Returns whether a teleport actually happened.
	private static boolean rescue(ServerPlayerEntity player) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid());
		if (maybeIsland.isEmpty()) {
			maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		}
		if (maybeIsland.isEmpty()) {
			// No island of their own and no Spawn island either: nowhere reasonable to rescue
			// them to.
			return false;
		}

		Island island = maybeIsland.get();
		ServerWorld world = player.getServer().getWorld(island.getDimension());
		if (world == null) {
			return false;
		}

		// homeLocation may have had a block broken out from under it since it was set: don't hand
		// the player a second fall right after rescuing them from the first one. Searches for the
		// nearest safe spot instead of blindly trusting the island's center — home defaults to
		// center on creation (see TeleportManagerImpl#requestHome), so if the player broke ground
		// right there, center is unsafe too, and falling back to it unchecked used to rescue them
		// into the exact same hole, looping forever. Persisted via updateHomeLocation so this
		// self-corrects once instead of re-triggering on every future fall.
		BlockPos destination = island.getHomeLocation();
		if (destination == null || !SafeLandingChecker.isSafe(world, destination)) {
			BlockPos searchOrigin = destination != null ? destination : island.getCenter();
			BlockPos safe = SafeLocationFinder.findNearestSafe(world, searchOrigin, SafeLocationFinder.DEFAULT_SEARCH_RADIUS, island.getBounds())
					.orElseGet(island::getCenter);
			IslandCoreMod.ISLAND_REGISTRY.updateHomeLocation(island.getIslandId(), safe);
			destination = safe;
		}

		// player.teleport(...) (ServerPlayerEntity#teleport -> requestTeleport, confirmed via
		// decompiled ServerPlayerEntity/ServerPlayNetworkHandler/Entity sources) only repositions
		// the entity — it never touches fallDistance or velocity. Without this, the player would
		// take fall damage for the entire void fall the instant they "land" at the rescue spot
		// (or, for the tick-based path, simply keep falling past the rescue point if velocity isn't
		// zeroed).
		if (BACKEND.teleport(player, world, destination)) {
			player.fallDistance = 0.0f;
			player.setVelocity(Vec3d.ZERO);
			player.velocityDirty = true;
			return true;
		}
		return false;
	}
}
