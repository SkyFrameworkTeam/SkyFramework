package com.skyframework.islandcore.player.rescue;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.teleport.SafeLandingChecker;
import com.skyframework.islandcore.teleport.TeleportBackend;
import com.skyframework.islandcore.teleport.VanillaTeleportBackend;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;

// Called from IslandCoreMod's third, independent ServerLivingEntityEvents.ALLOW_DAMAGE listener:
// an emergency rescue teleport for a player who falls out of the world over islandcore:islands (a
// flat void dimension — see data/islandcore/dimension/islands.json — so OUT_OF_WORLD damage is the
// normal way to fall off the edge of an unbuilt plot, not a rare edge case). Deliberately no
// countdown/cooldown like /island home has: this is a safety net triggered by the game, not an
// action the player chose to invoke.
public final class VoidRescueListener {

	private static final RegistryKey<World> ISLANDS_DIMENSION =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

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

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid());
		if (maybeIsland.isEmpty()) {
			maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		}
		if (maybeIsland.isEmpty()) {
			// No island of their own and no Spawn island either: nowhere reasonable to rescue
			// them to, so let the fall-out-of-world damage through as vanilla would.
			return true;
		}

		Island island = maybeIsland.get();
		ServerWorld world = player.getServer().getWorld(island.getDimension());
		if (world == null) {
			return true;
		}

		// homeLocation may have been set (before this fix existed) while standing over a block
		// that no longer has solid support underneath: don't hand the player a second fall right
		// after rescuing them from the first one.
		BlockPos destination = island.getHomeLocation();
		if (!SafeLandingChecker.isSafe(world, destination)) {
			destination = island.getCenter();
		}

		// player.teleport(...) (ServerPlayerEntity#teleport -> requestTeleport, confirmed via
		// decompiled ServerPlayerEntity/ServerPlayNetworkHandler/Entity sources) only repositions
		// the entity — it never touches fallDistance or velocity. Without this, the player would
		// take fall damage for the entire void fall the instant they "land" at the rescue spot.
		if (BACKEND.teleport(player, world, destination)) {
			player.fallDistance = 0.0f;
			player.setVelocity(Vec3d.ZERO);
			player.velocityDirty = true;
		}
		return false;
	}
}
