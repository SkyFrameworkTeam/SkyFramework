package com.skyframework.islandcore.protection;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.protection.flag.FlagRegistry;
import com.skyframework.islandcore.protection.flag.FlagResolver;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

// Called from mixin/RaidManagerMixin. DENY (the default) blocks a raid from starting for the
// island at pos; outside a claimed island — or outside islandcore:islands entirely — is always
// vanilla. Raids don't naturally occur in islandcore:islands (a flat void dimension with no
// generated villages), but a player-built village-like structure there is still possible, so this
// is checked regardless rather than assumed unreachable.
public final class RaidProtectionListener {

	private static final RegistryKey<World> ISLANDS_DIMENSION =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

	private RaidProtectionListener() {
	}

	public static boolean isRaidBlocked(ServerWorld world, BlockPos pos) {
		if (!world.getRegistryKey().equals(ISLANDS_DIMENSION)) {
			return false;
		}

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(pos);
		if (maybeIsland.isEmpty()) {
			return false;
		}

		return !FlagResolver.resolveGlobal(maybeIsland.get(), FlagRegistry.RAIDS);
	}
}
