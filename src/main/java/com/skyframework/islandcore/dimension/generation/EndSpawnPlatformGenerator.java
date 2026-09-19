package com.skyframework.islandcore.dimension.generation;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

// Guarantees an END_LIKE dimension's own spawn point is always safe to land on, independent of
// whatever the terrain generator happened to put there (real vanilla End terrain is genuinely sparse —
// see the "aterrizaje seguro" investigation — so world.getSpawnPos() landing on air/void isn't rare).
// Same spirit as BasicPlatformGenerator (islands), but much smaller and specific to a single fixed
// point rather than a whole buildable plot: a 3x3 end_stone floor with 2 clear blocks of air above
// every column, matching SafeLandingChecker's own safety criteria exactly so resolveSafeLanding's
// step1 (direct spawnPos check) always succeeds immediately here, with no fallback ever needed.
public final class EndSpawnPlatformGenerator {

	private static final int RADIUS = 1; // 3x3, centered on spawnPos.

	private EndSpawnPlatformGenerator() {
	}

	public static void generate(ServerWorld world, BlockPos spawnPos) {
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dz = -RADIUS; dz <= RADIUS; dz++) {
				BlockPos ground = spawnPos.add(dx, -1, dz);
				world.setBlockState(ground, Blocks.END_STONE.getDefaultState());
				world.setBlockState(ground.up(), Blocks.AIR.getDefaultState());
				world.setBlockState(ground.up(2), Blocks.AIR.getDefaultState());
			}
		}
	}
}
