package com.skyframework.islandcore.protection;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

// Called from mixin/PistonHandlerMixin. Kept as a plain class (rather than logic inline in the
// mixin) so the actual protection rule stays readable and separate from injected bytecode — same
// split ExplosionProtectionListener/ExplosionMixin already use.
public final class PistonProtectionListener {

	private static final RegistryKey<World> ISLANDS_DIMENSION =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

	private PistonProtectionListener() {
	}

	// posFrom is the piston block's OWN position, which decides which island's bounds apply — one
	// shared island for the whole push, not a per-block lookup, since the pushed blocks don't have
	// islands of their own to compare against, only a single final destination each has to stay
	// inside. A piston built inside a claimed island's bounds could otherwise use its extension
	// reach to place blocks past that island's own plot edge (unclaimed territory, another
	// island's plot, or the void) even though the piston itself sits on legitimately protected
	// ground — see IslandCommand's exploit report. Outside islandcore:islands, or when the piston
	// itself isn't on any claimed island, this is always vanilla, nothing to protect.
	public static boolean isPushBlocked(World world, BlockPos posFrom, Direction motionDirection, List<BlockPos> movedBlocks) {
		if (!world.getRegistryKey().equals(ISLANDS_DIMENSION)) {
			return false;
		}

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(posFrom);
		if (maybeIsland.isEmpty()) {
			return false;
		}

		Island island = maybeIsland.get();
		for (BlockPos moved : movedBlocks) {
			if (!island.getBounds().contains(moved.offset(motionDirection))) {
				return true;
			}
		}
		return false;
	}
}
