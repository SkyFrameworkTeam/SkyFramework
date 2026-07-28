package com.skyframework.islandcore.island.spatial;

import com.skyframework.islandcore.island.model.IslandBounds;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SpatialIndexImpl implements SpatialIndex {

	private final Map<Long, UUID> chunkIndex = new HashMap<>();

	@Override
	public void registerIsland(UUID islandId, IslandBounds plotBounds) {
		ChunkPos minChunk = new ChunkPos(plotBounds.min());
		ChunkPos maxChunk = new ChunkPos(plotBounds.max());

		for (int x = minChunk.x; x <= maxChunk.x; x++) {
			for (int z = minChunk.z; z <= maxChunk.z; z++) {
				chunkIndex.put(new ChunkPos(x, z).toLong(), islandId);
			}
		}
	}

	@Override
	public void unregisterIsland(UUID islandId) {
		// No inverse index needed: a plain scan is fine at dev-scale island counts.
		chunkIndex.values().removeIf(id -> id.equals(islandId));
	}

	@Override
	public Optional<UUID> getIslandIdAt(BlockPos pos) {
		return getIslandIdAt(new ChunkPos(pos));
	}

	@Override
	public Optional<UUID> getIslandIdAt(ChunkPos chunkPos) {
		return Optional.ofNullable(chunkIndex.get(chunkPos.toLong()));
	}
}
