package com.skyframework.islandcore.api.registry;

import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.island.model.IslandMember;
import com.skyframework.islandcore.island.model.IslandSetting;
import com.skyframework.islandcore.protection.flag.TriState;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.time.Instant;
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

	// Full cleanup: unregisters from the spatial index, releases the grid slot, deletes from
	// storage, and removes from the in-memory maps. Idempotent (a no-op if already deleted).
	// Called by IslandDeletionService as the final step of a confirmed island deletion.
	void deleteIsland(UUID islandId);

	// Marks the island DELETING and persists immediately, as a recovery checkpoint before
	// IslandDeletionService starts touching the world. Called by IslandDeletionService.
	void markIslandDeleting(UUID islandId);

	void addMember(UUID islandId, IslandMember member);

	void removeMember(UUID islandId, UUID playerUuid);

	// Physical expansion only (newSize must be > current islandSize and <= plotSize).
	// TODO: shrinking support in a future sprint.
	void resizeIsland(UUID islandId, int newSize);

	void updateHomeLocation(UUID islandId, BlockPos newHome);

	void updateCurrentBiomeId(UUID islandId, String biomeId);

	void updateLastBiomeChangeAt(UUID islandId, Instant instant);

	void updateIslandSetting(UUID islandId, IslandSetting setting, boolean value);

	void updateGlobalFlagOverride(UUID islandId, String flagId, TriState value);

	void updateRoleFlagOverride(UUID islandId, String flagId, TriState value);

	// The 4-way preset shortcut ("/island flags preset", FlagSetPresetC2S): preset is one of
	// "nadie"/"miembros"/"aliados"/"todos" (see protection.flag.FlagPreset). Throws
	// IllegalArgumentException if flagId doesn't resolve to a ROLE_BASED flag, or if preset isn't
	// one of the 4 valid names. A no-op if islandId doesn't resolve to a loaded island (mirrors
	// every other update* method here).
	void applyFlagPreset(UUID islandId, String flagId, String preset);

	// The 4-way preset shortcut for exception groups ("/island exceptions preset",
	// ExceptionGroupSetPresetC2S) — exact mirror of applyFlagPreset above, but for
	// ExceptionGroupRegistry groups instead of ROLE_BASED flags. Throws IllegalArgumentException if
	// groupId doesn't resolve to a registered ExceptionGroup, or preset isn't one of the 4 valid
	// names. A no-op if islandId doesn't resolve to a loaded island (mirrors every other update*
	// method here).
	void applyExceptionGroupPreset(UUID islandId, String groupId, String preset);

	// Saves every currently loaded island; used as an extra safety net on server shutdown
	// (individual mutations already persist themselves).
	void saveAll();
}
