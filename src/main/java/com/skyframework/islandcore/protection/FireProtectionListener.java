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

// Called from mixin/FireSpreadMixin and mixin/LightningFireMixin.
public final class FireProtectionListener {

	private static final RegistryKey<World> ISLANDS_DIMENSION =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

	private FireProtectionListener() {
	}

	public static boolean isFireSpreadAllowed(World world, BlockPos pos) {
		if (!world.getRegistryKey().equals(ISLANDS_DIMENSION)) {
			return true;
		}

		// Inside the islands dimension, no island claims pos: nothing to protect there,
		// behaves like vanilla (allowed).
		Optional<Island> island = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(pos);
		return island.map(value -> FlagResolver.resolveGlobal(value, FlagRegistry.FIRE_SPREAD)).orElse(true);
	}
}
