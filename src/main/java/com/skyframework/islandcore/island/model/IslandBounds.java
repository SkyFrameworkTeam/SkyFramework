package com.skyframework.islandcore.island.model;

import net.minecraft.util.math.BlockPos;

public record IslandBounds(BlockPos min, BlockPos max) {

	public boolean contains(BlockPos pos) {
		return pos.getX() >= min.getX() && pos.getX() <= max.getX()
				&& pos.getY() >= min.getY() && pos.getY() <= max.getY()
				&& pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
	}

	public boolean intersects(IslandBounds other) {
		return min.getX() <= other.max.getX() && max.getX() >= other.min.getX()
				&& min.getY() <= other.max.getY() && max.getY() >= other.min.getY()
				&& min.getZ() <= other.max.getZ() && max.getZ() >= other.min.getZ();
	}
}
