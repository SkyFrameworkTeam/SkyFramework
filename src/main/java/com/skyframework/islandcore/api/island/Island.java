package com.skyframework.islandcore.api.island;

import com.skyframework.islandcore.island.model.IslandBounds;
import com.skyframework.islandcore.island.model.IslandMember;
import com.skyframework.islandcore.island.model.IslandRole;
import com.skyframework.islandcore.island.model.IslandSetting;
import com.skyframework.islandcore.island.model.IslandType;
import com.skyframework.islandcore.protection.flag.TriState;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface Island {

	// Represents "the server" as an island owner, used for the Spawn island.
	UUID SERVER_OWNER_UUID = new UUID(0, 0);

	UUID getIslandId();

	UUID getOwnerUuid();

	RegistryKey<World> getDimension();

	int getGridX();

	int getGridZ();

	BlockPos getCenter();

	IslandBounds getBounds();

	IslandBounds getPlotBounds();

	int getIslandSize();

	int getPlotSize();

	IslandType getIslandType();

	BlockPos getHomeLocation();

	// Never null: defaults to IslandData.DEFAULT_BIOME_ID ("minecraft:the_void", the flat
	// generator biome the islandcore:islands dimension itself uses — see islands.json) for an
	// island that has never had /island biome used on it, then tracks whatever was last applied.
	String getCurrentBiomeId();

	// Null if the biome has never been changed via /island biome.
	Instant getLastBiomeChangeAt();

	IslandState getState();

	Set<IslandMember> getMembers();

	// Read-only: writing a setting is only exposed via the registry (updateIslandSetting), not here.
	boolean getSetting(IslandSetting setting);

	// Read-only: writing an override is only exposed via the registry (updateGlobalFlagOverride/
	// updateRoleFlagOverride/updateExceptionGroupOverride), not here. Never null; TriState.DEFAULT
	// means "no island-level override for this flag" (see FlagResolver for the rest of the chain).
	TriState getGlobalFlagOverride(String flagId);

	TriState getRoleFlagOverride(String flagId, IslandRole role);

	// Null means "no island-level override for this exception group" (falls back to the group's own
	// defaultEnabled — see ExceptionResolver), as opposed to a real true/false override.
	Boolean getExceptionGroupOverride(String groupId);

	IslandRole getRoleOf(UUID playerUuid);

	boolean hasPermission(UUID playerUuid, IslandPermission permission);

	Instant getCreatedAt();

	Instant getUpdatedAt();
}
