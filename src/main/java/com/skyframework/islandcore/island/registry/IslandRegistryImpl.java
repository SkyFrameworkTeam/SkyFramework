package com.skyframework.islandcore.island.registry;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.island.IslandState;
import com.skyframework.islandcore.api.registry.IslandRegistryApi;
import com.skyframework.islandcore.island.generation.BasicPlatformGenerator;
import com.skyframework.islandcore.island.grid.GridAllocator;
import com.skyframework.islandcore.island.grid.GridAllocatorImpl;
import com.skyframework.islandcore.island.grid.GridCoordinate;
import com.skyframework.islandcore.island.model.IslandBounds;
import com.skyframework.islandcore.island.model.IslandData;
import com.skyframework.islandcore.island.model.IslandMember;
import com.skyframework.islandcore.island.model.IslandSetting;
import com.skyframework.islandcore.island.model.IslandType;
import com.skyframework.islandcore.island.spatial.SpatialIndex;
import com.skyframework.islandcore.island.spatial.SpatialIndexImpl;
import com.skyframework.islandcore.storage.IslandStorage;
import com.skyframework.islandcore.storage.nbt.NbtIslandStorage;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Grid allocation and spatial lookup are real (Sprint 4). Metadata persistence is real (Sprint 5).
public class IslandRegistryImpl implements IslandRegistryApi {

	// TODO: come from LuckPerms/config in a future sprint.
	private static final int DEFAULT_ISLAND_SIZE = 10;

	// plotBounds fills most of a grid cell, leaving a small visual gap between neighboring islands.
	private static final int PLOT_MARGIN = 10;

	// Matches the islandcore:islands_type datapack config from Sprint 1 (min_y=-64, height=384).
	private static final int MIN_Y = -64;
	private static final int MAX_Y = 319;

	private final Map<UUID, IslandData> islandsById = new LinkedHashMap<>();
	private final Map<UUID, UUID> islandIdByOwner = new HashMap<>();

	private final BasicPlatformGenerator platformGenerator = new BasicPlatformGenerator();
	private final GridAllocator gridAllocator = new GridAllocatorImpl();
	private final SpatialIndex spatialIndex = new SpatialIndexImpl();

	// onInitialize() runs before the MinecraftServer exists, so the reference is captured lazily on server start.
	private MinecraftServer server;

	private IslandStorage storage;

	public IslandRegistryImpl() {
		// NOTE: the task brief for Sprint 5 described this listener as living in IslandCoreMod.java,
		// but it was already here since Sprint 3 (it's where the MinecraftServer reference is captured,
		// which createSpawnIsland also needs). Kept it here rather than splitting that state across
		// two classes; initializeStorage() is still a public method callable from elsewhere too.
		ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> {
			this.server = startedServer;
			initializeStorage(startedServer.getSavePath(WorldSavePath.ROOT));
		});
	}

	public void initializeStorage(Path worldSaveDir) {
		storage = new NbtIslandStorage(worldSaveDir.resolve("islandcore").resolve("islands"));

		List<UUID> interruptedDeletions = new ArrayList<>();

		for (IslandData island : storage.loadAll()) {
			islandsById.put(island.getIslandId(), island);
			islandIdByOwner.put(island.getOwnerUuid(), island.getIslandId());
			gridAllocator.occupySlot(new GridCoordinate(island.getGridX(), island.getGridZ()));
			spatialIndex.registerIsland(island.getIslandId(), island.getPlotBounds());

			if (island.getState() == IslandState.DELETING) {
				interruptedDeletions.add(island.getIslandId());
			}
		}

		// Resume any deletion that was mid-flight when the server last stopped. By this point
		// IslandCoreMod.DELETION_SERVICE is already set (onInitialize() finished long before
		// this SERVER_STARTED-triggered method runs), and executeDeletion() is idempotent.
		for (UUID islandId : interruptedDeletions) {
			IslandCoreMod.DELETION_SERVICE.executeDeletion(islandId);
		}
	}

	@Override
	public Optional<Island> getIsland(UUID islandId) {
		return Optional.ofNullable(islandsById.get(islandId));
	}

	@Override
	public Optional<Island> getIslandByOwner(UUID playerUuid) {
		UUID islandId = islandIdByOwner.get(playerUuid);
		if (islandId == null) {
			return Optional.empty();
		}
		return getIsland(islandId);
	}

	@Override
	public Optional<Island> getIslandAt(BlockPos position) {
		return spatialIndex.getIslandIdAt(position).flatMap(this::getIsland);
	}

	@Override
	public boolean exists(UUID islandId) {
		return islandsById.containsKey(islandId);
	}

	@Override
	public Collection<Island> getAllIslands() {
		return List.copyOf(islandsById.values());
	}

	@Override
	public Island createIsland(UUID ownerUuid, RegistryKey<World> dimension) {
		GridCoordinate coordinate = gridAllocator.allocateNextSlot();
		BlockPos center = gridAllocator.gridToWorldPos(coordinate);
		int plotSize = GridAllocator.CELL_SIZE - PLOT_MARGIN;

		IslandData island = createIslandInternal(
				ownerUuid, dimension, coordinate.gridX(), coordinate.gridZ(), center, DEFAULT_ISLAND_SIZE, plotSize);

		spatialIndex.registerIsland(island.getIslandId(), island.getPlotBounds());
		saveIfStorageReady(island);

		return island;
	}

	@Override
	public Island createSpawnIsland(RegistryKey<World> dimension, int size) {
		// (0,0) is fixed explicitly rather than requested from the allocator, since that cell is pre-reserved.
		GridCoordinate coordinate = new GridCoordinate(0, 0);
		BlockPos center = gridAllocator.gridToWorldPos(coordinate);
		int plotSize = GridAllocator.CELL_SIZE - PLOT_MARGIN;

		IslandData island = createIslandInternal(
				Island.SERVER_OWNER_UUID, dimension, coordinate.gridX(), coordinate.gridZ(), center, size, plotSize);

		spatialIndex.registerIsland(island.getIslandId(), island.getPlotBounds());
		saveIfStorageReady(island);

		// Unlike createIsland (whose caller generates the platform), the spawn island has no
		// per-player command step for that, so it is generated here as part of creation.
		if (server != null) {
			ServerWorld world = server.getWorld(dimension);
			if (world != null) {
				platformGenerator.generate(world, center, size);
			}
		}

		return island;
	}

	private IslandData createIslandInternal(
			UUID ownerUuid,
			RegistryKey<World> dimension,
			int gridX,
			int gridZ,
			BlockPos center,
			int islandSize,
			int plotSize
	) {
		UUID existingIslandId = islandIdByOwner.get(ownerUuid);
		if (existingIslandId != null) {
			throw new IllegalStateException("Player " + ownerUuid + " already owns an island: " + existingIslandId);
		}

		UUID islandId = UUID.randomUUID();

		IslandBounds bounds = boundsAround(center, islandSize);
		IslandBounds plotBounds = boundsAround(center, plotSize);

		Instant now = Instant.now();

		IslandData island = new IslandData(
				islandId,
				ownerUuid,
				dimension,
				gridX,
				gridZ,
				center,
				bounds,
				plotBounds,
				islandSize,
				plotSize,
				IslandType.PLAINS,
				center,
				IslandState.ACTIVE,
				now,
				IslandData.DEFAULT_BIOME_ID
		);

		islandsById.put(islandId, island);
		islandIdByOwner.put(ownerUuid, islandId);

		return island;
	}

	@Override
	public void deleteIsland(UUID islandId) {
		IslandData island = islandsById.get(islandId);
		if (island == null) {
			// Already gone: idempotent no-op.
			return;
		}

		spatialIndex.unregisterIsland(islandId);
		gridAllocator.releaseSlot(new GridCoordinate(island.getGridX(), island.getGridZ()));
		if (storage != null) {
			storage.delete(islandId);
		}

		islandsById.remove(islandId);
		islandIdByOwner.remove(island.getOwnerUuid());
	}

	@Override
	public void markIslandDeleting(UUID islandId) {
		IslandData island = islandsById.get(islandId);
		if (island != null) {
			island.setState(IslandState.DELETING);
			saveIfStorageReady(island);
		}
	}

	@Override
	public void addMember(UUID islandId, IslandMember member) {
		IslandData island = islandsById.get(islandId);
		if (island != null) {
			island.addMember(member);
			saveIfStorageReady(island);
		}
	}

	@Override
	public void removeMember(UUID islandId, UUID playerUuid) {
		IslandData island = islandsById.get(islandId);
		if (island != null) {
			island.removeMember(playerUuid);
			saveIfStorageReady(island);
		}
	}

	@Override
	public void updateHomeLocation(UUID islandId, BlockPos newHome) {
		IslandData island = islandsById.get(islandId);
		if (island != null) {
			island.setHomeLocation(newHome);
			saveIfStorageReady(island);
		}
	}

	@Override
	public void updateCurrentBiomeId(UUID islandId, String biomeId) {
		IslandData island = islandsById.get(islandId);
		if (island != null) {
			island.setCurrentBiomeId(biomeId);
			saveIfStorageReady(island);
		}
	}

	@Override
	public void updateLastBiomeChangeAt(UUID islandId, Instant instant) {
		IslandData island = islandsById.get(islandId);
		if (island != null) {
			island.setLastBiomeChangeAt(instant);
			saveIfStorageReady(island);
		}
	}

	@Override
	public void updateIslandSetting(UUID islandId, IslandSetting setting, boolean value) {
		IslandData island = islandsById.get(islandId);
		if (island != null) {
			island.setSetting(setting, value);
			saveIfStorageReady(island);
		}
	}

	@Override
	public void resizeIsland(UUID islandId, int newSize) {
		IslandData island = islandsById.get(islandId);
		if (island == null) {
			throw new IllegalArgumentException("No existe ninguna isla con id: " + islandId);
		}

		int oldSize = island.getIslandSize();
		if (newSize <= oldSize) {
			// TODO: support shrinking in a future sprint.
			throw new IllegalArgumentException("Solo se permite ampliar el tamaño de la isla, no reducirlo");
		}
		if (newSize > island.getPlotSize()) {
			throw new IllegalArgumentException("El nuevo tamaño excede la parcela reservada para esta isla");
		}

		if (server != null) {
			ServerWorld world = server.getWorld(island.getDimension());
			if (world != null) {
				platformGenerator.generateExpansion(world, island.getCenter(), oldSize, newSize);
			}
		}

		// setIslandSize/setBounds already call touch() internally, refreshing updatedAt.
		island.setIslandSize(newSize);
		island.setBounds(boundsAround(island.getCenter(), newSize));

		saveIfStorageReady(island);
	}

	@Override
	public void saveAll() {
		if (storage == null) {
			return;
		}
		for (IslandData island : islandsById.values()) {
			storage.save(island);
		}
	}

	private void saveIfStorageReady(IslandData island) {
		if (storage != null) {
			storage.save(island);
		}
	}

	private static IslandBounds boundsAround(BlockPos center, int size) {
		int half = size / 2;
		BlockPos min = new BlockPos(center.getX() - half, MIN_Y, center.getZ() - half);
		BlockPos max = new BlockPos(center.getX() + half, MAX_Y, center.getZ() + half);
		return new IslandBounds(min, max);
	}
}
