package com.skyframework.islandcore.protection;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.protection.flag.FlagRegistry;
import com.skyframework.islandcore.protection.flag.FlagResolver;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Optional;

// Called from IslandCoreMod's second ServerLivingEntityEvents.ALLOW_DAMAGE listener.
//
// Deliberately asymmetric between the two toggles (explicit design decision, not an oversight):
//   - MOB_DAMAGE (at least one side isn't a player): an attacking player with the ENTITIES
//     permission always gets to attack/manage their own animals or defend against mobs,
//     regardless of MOB_DAMAGE — this isn't "outside damage" being let in, it's the owner/a
//     trusted member acting legitimately on the island they already have rights on.
//   - PVP_DAMAGE (both sides are players): NO exception, not even for the owner or someone with
//     ENTITIES. The outcome depends solely on the island's resolved PVP_DAMAGE value — pvp=false
//     means nobody can hit anybody there, pvp=true means anybody can hit anybody. This makes PVP
//     symmetric: the owner can't rely on ENTITIES to sidestep their own PVP setting.
public final class DamageProtectionListener {

	private static final RegistryKey<World> ISLANDS_DIMENSION =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

	private DamageProtectionListener() {
	}

	public static boolean isDamageAllowed(World world, LivingEntity victim, DamageSource source) {
		if (!world.getRegistryKey().equals(ISLANDS_DIMENSION)) {
			return true;
		}

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(victim.getBlockPos());
		if (maybeIsland.isEmpty()) {
			// Unclaimed zone within the dimension: nothing to protect there.
			return true;
		}

		Entity attacker = source.getAttacker();
		if (attacker == null) {
			// No attacker: fall damage, lava, drowning, starvation, etc. Not part of this system.
			return true;
		}

		Island island = maybeIsland.get();

		if (attacker instanceof PlayerEntity && victim instanceof PlayerEntity) {
			// PVP is symmetric: no ENTITIES bypass here, not even for the owner — see class javadoc.
			return FlagResolver.resolveGlobal(island, FlagRegistry.PVP_DAMAGE);
		}

		// At least one side is not a player: an attacking player with ENTITIES may always
		// attack/manage their own animals or defend against mobs, regardless of MOB_DAMAGE.
		if (attacker instanceof PlayerEntity attackerPlayer
				&& FlagResolver.resolveForPlayer(island, attackerPlayer.getUuid(), FlagRegistry.ENTITIES)) {
			return true;
		}

		return FlagResolver.resolveGlobal(island, FlagRegistry.MOB_DAMAGE);
	}
}
