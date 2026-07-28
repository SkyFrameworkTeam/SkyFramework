package com.skyframework.islandcore.island.spatial;

import com.skyframework.islandcore.island.model.IslandBounds;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.Optional;
import java.util.UUID;

public interface SpatialIndex {

	void registerIsland(UUID islandId, IslandBounds plotBounds);

	void unregisterIsland(UUID islandId);

	Optional<UUID> getIslandIdAt(BlockPos pos);

	Optional<UUID> getIslandIdAt(ChunkPos chunkPos);
}
