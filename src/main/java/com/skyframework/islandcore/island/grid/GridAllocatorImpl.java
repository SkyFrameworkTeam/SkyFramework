package com.skyframework.islandcore.island.grid;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridAllocatorImpl implements GridAllocator {

	private static final int WORLD_Y = 100;

	private final Map<GridCoordinate, GridSlotState> slots = new HashMap<>();

	public GridAllocatorImpl() {
		// (0,0) is reserved for the Spawn island and must never be handed out by allocateNextSlot().
		occupySlot(new GridCoordinate(0, 0));
	}

	@Override
	public GridCoordinate allocateNextSlot() {
		int radius = 1;
		while (true) {
			for (GridCoordinate candidate : ringCoordinates(radius)) {
				if (isFree(candidate)) {
					slots.put(candidate, GridSlotState.OCCUPIED);
					return candidate;
				}
			}
			radius++;
		}
	}

	@Override
	public void releaseSlot(GridCoordinate coordinate) {
		slots.put(coordinate, GridSlotState.FREE);
	}

	@Override
	public void occupySlot(GridCoordinate coordinate) {
		slots.put(coordinate, GridSlotState.OCCUPIED);
	}

	@Override
	public BlockPos gridToWorldPos(GridCoordinate coordinate) {
		int worldX = coordinate.gridX() * CELL_SIZE;
		int worldZ = coordinate.gridZ() * CELL_SIZE;
		return new BlockPos(worldX, WORLD_Y, worldZ);
	}

	private boolean isFree(GridCoordinate coordinate) {
		return slots.getOrDefault(coordinate, GridSlotState.FREE) == GridSlotState.FREE;
	}

	// Growing square ring around (0,0): all cells at Chebyshev distance == radius.
	private static List<GridCoordinate> ringCoordinates(int radius) {
		List<GridCoordinate> coordinates = new ArrayList<>();

		for (int x = -radius; x <= radius; x++) {
			coordinates.add(new GridCoordinate(x, -radius));
			coordinates.add(new GridCoordinate(x, radius));
		}
		for (int z = -radius + 1; z <= radius - 1; z++) {
			coordinates.add(new GridCoordinate(-radius, z));
			coordinates.add(new GridCoordinate(radius, z));
		}

		return coordinates;
	}
}
