package com.skyframework.islandcore.island.generation;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

// TODO: Placeholder temporal — sustituir por sistema de plantillas configurables por IslandType en sprint futuro.
public class BasicPlatformGenerator {

	public void generate(ServerWorld world, BlockPos center, int size) {
		int y = center.getY() - 1;
		SquareRange range = squareRange(center, size);

		for (int x = range.minX(); x <= range.maxX(); x++) {
			for (int z = range.minZ(); z <= range.maxZ(); z++) {
				boolean isEdge = x == range.minX() || x == range.maxX() || z == range.minZ() || z == range.maxZ();
				var blockState = isEdge ? Blocks.SMOOTH_STONE.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState();
				world.setBlockState(new BlockPos(x, y, z), blockState);
			}
		}
	}

	// Assumes newSize > oldSize (expansion only), never called for a reduction: that validation
	// belongs to the service layer (IslandRegistryImpl.resizeIsland), not to this method.
	public void generateExpansion(ServerWorld world, BlockPos center, int oldSize, int newSize) {
		int y = center.getY() - 1;
		SquareRange oldRange = squareRange(center, oldSize);
		SquareRange newRange = squareRange(center, newSize);

		for (int x = newRange.minX(); x <= newRange.maxX(); x++) {
			for (int z = newRange.minZ(); z <= newRange.maxZ(); z++) {
				if (oldRange.contains(x, z)) {
					// Already part of the built island: leave it untouched.
					continue;
				}

				boolean isEdge = x == newRange.minX() || x == newRange.maxX() || z == newRange.minZ() || z == newRange.maxZ();
				var blockState = isEdge ? Blocks.SMOOTH_STONE.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState();
				world.setBlockState(new BlockPos(x, y, z), blockState);
			}
		}

		convertOldEdgeToGrass(world, oldRange, y);
	}

	// The old outer perimeter (oldSize) stopped being the island's edge once it grew, so it
	// should read as grass like the rest of the interior — but only where the player hasn't
	// already built over it; any block other than the original smooth_stone is left untouched.
	private static void convertOldEdgeToGrass(ServerWorld world, SquareRange oldRange, int y) {
		for (int x = oldRange.minX(); x <= oldRange.maxX(); x++) {
			for (int z = oldRange.minZ(); z <= oldRange.maxZ(); z++) {
				boolean wasEdge = x == oldRange.minX() || x == oldRange.maxX() || z == oldRange.minZ() || z == oldRange.maxZ();
				if (!wasEdge) {
					continue;
				}

				BlockPos pos = new BlockPos(x, y, z);
				if (world.getBlockState(pos).getBlock() == Blocks.SMOOTH_STONE) {
					world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState());
				}
			}
		}
	}

	private static SquareRange squareRange(BlockPos center, int size) {
		int half = size / 2;
		return new SquareRange(center.getX() - half, center.getX() + half, center.getZ() - half, center.getZ() + half);
	}

	private record SquareRange(int minX, int maxX, int minZ, int maxZ) {
		boolean contains(int x, int z) {
			return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
		}
	}
}
