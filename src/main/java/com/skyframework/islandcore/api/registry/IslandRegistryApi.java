package com.skyframework.islandcore.api.registry;

import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.island.model.IslandMember;
import com.skyframework.islandcore.island.model.IslandSetting;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IslandRegistryApi {

	Optional<Island> getIsland(UUID islandId);

	Optional<Island> getIslandByOwner(UUID playerUuid);

	Optional<Island> getIslandAt(BlockPos position);

	boolean exists(UUID islandId);

	Collection<Island> getAllIslands();

	Island createIsland(UUID ownerUuid, RegistryKey<World> dimension);

	Island createSpawnIsland(RegistryKey<World> dimension, int size);

	// TODO: not yet paired with storage/grid/spatial-index cleanup; that's a future sprint.
	void deleteIsland(UUID islandId);

	void addMember(UUID islandId, IslandMember member);

	void removeMember(UUID islandId, UUID playerUuid);

	// Physical expansion only (newSize must be > current islandSize and <= plotSize).
	// TODO: shrinking support in a future sprint.
	void resizeIsland(UUID islandId, int newSize);

	void updateHomeLocation(UUID islandId, BlockPos newHome);

	void updateIslandSetting(UUID islandId, IslandSetting setting, boolean value);

	// Saves every currently loaded island; used as an extra safety net on server shutdown
	// (individual mutations already persist themselves).
	void saveAll();
}
