package com.skyframework.islandcore.protection;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.protection.flag.FlagRegistry;
import com.skyframework.islandcore.protection.flag.FlagResolver;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

// Called from mixin/FarmlandTrampleMixin. Exact same shape as ExplosionProtectionListener: DENY
// (the default) blocks farmland trampling into dirt for the island at pos; outside a claimed
// island — or outside islandcore:islands entirely — is always vanilla.
public final class CropTrampleProtectionListener {

	private static final RegistryKey<World> ISLANDS_DIMENSION =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

	private CropTrampleProtectionListener() {
	}

	public static boolean isTrampleBlocked(World world, BlockPos pos) {
		if (!world.getRegistryKey().equals(ISLANDS_DIMENSION)) {
			return false;
		}

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(pos);
		if (maybeIsland.isEmpty()) {
			return false;
		}

		return !FlagResolver.resolveGlobal(maybeIsland.get(), FlagRegistry.CROP_TRAMPLE);
	}
}
