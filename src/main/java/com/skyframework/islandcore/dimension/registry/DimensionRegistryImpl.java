package com.skyframework.islandcore.dimension.registry;

import com.skyframework.islandcore.dimension.model.DimensionData;
import com.skyframework.islandcore.dimension.model.DimensionDefinition;
import com.skyframework.islandcore.dimension.model.DimensionGeneratorStyle;
import com.skyframework.islandcore.dimension.model.DimensionState;
import com.skyframework.islandcore.dimension.runtime.DimensionRuntimeProvider;
import com.skyframework.islandcore.dimension.storage.DimensionStorage;
import com.skyframework.islandcore.dimension.storage.NbtDimensionStorage;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Deliberately independent of the island/ package (no imports from it): this is a standalone
// module, following the same registry+storage shape as IslandRegistryImpl but for dimensions.
public class DimensionRegistryImpl implements DimensionRegistry {

	private final Map<Identifier, DimensionData> dimensionsById = new HashMap<>();
	private final DimensionRuntimeProvider runtimeProvider;

	private DimensionStorage storage;

	// onInitialize() runs before the MinecraftServer exists, so the reference is captured lazily on server start.
	private MinecraftServer server;

	public DimensionRegistryImpl(DimensionRuntimeProvider runtimeProvider) {
		this.runtimeProvider = runtimeProvider;

		// Self-contained SERVER_STARTED listener, same pattern as IslandRegistryImpl — this
		// package can't share that listener since it must not import anything from island/.
		ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> {
			this.server = startedServer;
			initializeStorage(startedServer.getSavePath(WorldSavePath.ROOT));
		});
	}

	public void initializeStorage(Path worldSaveDir) {
		storage = new NbtDimensionStorage(worldSaveDir.resolve("islandcore").resolve("dimensions"));

		for (DimensionData dimension : storage.loadAll()) {
			dimensionsById.put(dimension.getId(), dimension);

			// This is the piece Multiworld/WorldManager don't guarantee reliably: every saved
			// dimension is re-materialized right here, on every boot, rather than leaving that up
			// to a separate step that might not run.
			if (server != null) {
				runtimeProvider.createOrLoadWorld(dimension, server);
			}
		}
	}

	@Override
	public Optional<DimensionDefinition> getDimension(Identifier id) {
		return Optional.ofNullable(dimensionsById.get(id));
	}

	@Override
	public Collection<DimensionDefinition> getAllDimensions() {
		return List.copyOf(dimensionsById.values());
	}

	@Override
	public boolean exists(Identifier id) {
		return dimensionsById.containsKey(id);
	}

	@Override
	public DimensionDefinition createDimension(Identifier id, String displayName, DimensionGeneratorStyle style, long seed) {
		if (dimensionsById.containsKey(id)) {
			throw new IllegalStateException("A dimension with id " + id + " already exists");
		}

		Instant now = Instant.now();
		DimensionData dimension = new DimensionData(id, displayName, style, seed, DimensionState.ACTIVE, now, now);

		dimensionsById.put(id, dimension);
		saveIfStorageReady(dimension);

		if (server != null) {
			runtimeProvider.createOrLoadWorld(dimension, server);
		}

		return dimension;
	}

	@Override
	public void deleteDimension(Identifier id) {
		throw new UnsupportedOperationException("Pendiente del Sprint 17");
	}

	@Override
	public void regenerateDimension(Identifier id, long newSeed) {
		throw new UnsupportedOperationException("Pendiente del Sprint 17");
	}

	private void saveIfStorageReady(DimensionData dimension) {
		if (storage != null) {
			storage.save(dimension);
		}
	}
}
