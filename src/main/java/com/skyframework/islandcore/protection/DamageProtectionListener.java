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
// An attacker who has the ENTITIES permission on the island always gets to attack, regardless
// of PVP_DAMAGE/MOB_DAMAGE: this isn't "outside damage" being let in, it's the owner/a trusted
// member acting legitimately on the island they already have rights on.
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

		if (attacker instanceof PlayerEntity attackerPlayer
				&& FlagResolver.resolveForPlayer(island, attackerPlayer.getUuid(), FlagRegistry.ENTITIES)) {
			return true;
		}

		if (attacker instanceof PlayerEntity && victim instanceof PlayerEntity) {
			return FlagResolver.resolveGlobal(island, FlagRegistry.PVP_DAMAGE);
		}

		// At least one side is not a player.
		return FlagResolver.resolveGlobal(island, FlagRegistry.MOB_DAMAGE);
	}
}
