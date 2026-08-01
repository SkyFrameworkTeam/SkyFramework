package com.skyframework.islandcore.island.model;

import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.island.IslandPermission;
import com.skyframework.islandcore.api.island.IslandState;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class IslandData implements Island {

	// The islandcore:islands dimension's own flat-generator biome (see
	// data/islandcore/dimension/islands.json) — the actual physical biome a freshly built
	// island's plot sits in before any /island biome change is ever applied, and what
	// currentBiomeId falls back to for islands persisted before this field existed.
	public static final String DEFAULT_BIOME_ID = "minecraft:the_void";

	private final UUID islandId;
	private final UUID ownerUuid;
	private final RegistryKey<World> dimension;
	private final int gridX;
	private final int gridZ;

	private BlockPos center;
	private IslandBounds bounds;
	private IslandBounds plotBounds;
	private int islandSize;
	private int plotSize;
	private IslandType islandType;
	private BlockPos homeLocation;
	private String currentBiomeId;
	private Instant lastBiomeChangeAt;
	private IslandState state;
	private final Set<IslandMember> members = new LinkedHashSet<>();
	private final Map<IslandSetting, Boolean> settings = new EnumMap<>(IslandSetting.class);

	private final Instant createdAt;
	private Instant updatedAt;

	public IslandData(
			UUID islandId,
			UUID ownerUuid,
			RegistryKey<World> dimension,
			int gridX,
			int gridZ,
			BlockPos center,
			IslandBounds bounds,
			IslandBounds plotBounds,
			int islandSize,
			int plotSize,
			IslandType islandType,
			BlockPos homeLocation,
			IslandState state,
			Instant createdAt,
			String currentBiomeId
	) {
		this.islandId = islandId;
		this.ownerUuid = ownerUuid;
		this.dimension = dimension;
		this.gridX = gridX;
		this.gridZ = gridZ;
		this.center = center;
		this.bounds = bounds;
		this.plotBounds = plotBounds;
		this.islandSize = islandSize;
		this.plotSize = plotSize;
		this.islandType = islandType;
		this.homeLocation = homeLocation;
		this.currentBiomeId = currentBiomeId;
		this.lastBiomeChangeAt = null;
		this.state = state;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	// Reconstruction constructor used when loading persisted islands from storage: unlike the
	// primary constructor, updatedAt/members/settings are not freshly initialized but restored as-is.
	public IslandData(
			UUID islandId,
			UUID ownerUuid,
			RegistryKey<World> dimension,
			int gridX,
			int gridZ,
			BlockPos center,
			IslandBounds bounds,
			IslandBounds plotBounds,
			int islandSize,
			int plotSize,
			IslandType islandType,
			BlockPos homeLocation,
			IslandState state,
			Instant createdAt,
			Instant updatedAt,
			Collection<IslandMember> members,
			Map<IslandSetting, Boolean> settings,
			Instant lastBiomeChangeAt,
			String currentBiomeId
	) {
		this(islandId, ownerUuid, dimension, gridX, gridZ, center, bounds, plotBounds,
				islandSize, plotSize, islandType, homeLocation, state, createdAt, currentBiomeId);
		this.updatedAt = updatedAt;
		this.members.addAll(members);
		this.settings.putAll(settings);
		this.lastBiomeChangeAt = lastBiomeChangeAt;
	}

	@Override
	public UUID getIslandId() {
		return islandId;
	}

	@Override
	public UUID getOwnerUuid() {
		return ownerUuid;
	}

	@Override
	public RegistryKey<World> getDimension() {
		return dimension;
	}

	@Override
	public int getGridX() {
		return gridX;
	}

	@Override
	public int getGridZ() {
		return gridZ;
	}

	@Override
	public BlockPos getCenter() {
		return center;
	}

	public void setCenter(BlockPos center) {
		this.center = center;
		touch();
	}

	@Override
	public IslandBounds getBounds() {
		return bounds;
	}

	public void setBounds(IslandBounds bounds) {
		this.bounds = bounds;
		touch();
	}

	@Override
	public IslandBounds getPlotBounds() {
		return plotBounds;
	}

	public void setPlotBounds(IslandBounds plotBounds) {
		this.plotBounds = plotBounds;
		touch();
	}

	@Override
	public int getIslandSize() {
		return islandSize;
	}

	public void setIslandSize(int islandSize) {
		this.islandSize = islandSize;
		touch();
	}

	@Override
	public int getPlotSize() {
		return plotSize;
	}

	public void setPlotSize(int plotSize) {
		this.plotSize = plotSize;
		touch();
	}

	@Override
	public IslandType getIslandType() {
		return islandType;
	}

	public void setIslandType(IslandType islandType) {
		this.islandType = islandType;
		touch();
	}

	@Override
	public BlockPos getHomeLocation() {
		return homeLocation;
	}

	public void setHomeLocation(BlockPos homeLocation) {
		this.homeLocation = homeLocation;
		touch();
	}

	@Override
	public String getCurrentBiomeId() {
		return currentBiomeId;
	}

	public void setCurrentBiomeId(String currentBiomeId) {
		this.currentBiomeId = currentBiomeId;
		touch();
	}

	@Override
	public Instant getLastBiomeChangeAt() {
		return lastBiomeChangeAt;
	}

	public void setLastBiomeChangeAt(Instant lastBiomeChangeAt) {
		this.lastBiomeChangeAt = lastBiomeChangeAt;
		touch();
	}

	@Override
	public IslandState getState() {
		return state;
	}

	public void setState(IslandState state) {
		this.state = state;
		touch();
	}

	@Override
	public Set<IslandMember> getMembers() {
		return Collections.unmodifiableSet(members);
	}

	public void addMember(IslandMember member) {
		// Upsert by playerUuid: replace any existing entry so a re-trust doesn't leave a stale duplicate.
		members.removeIf(existing -> existing.playerUuid().equals(member.playerUuid()));
		members.add(member);
		touch();
	}

	public void removeMember(UUID playerUuid) {
		members.removeIf(existing -> existing.playerUuid().equals(playerUuid));
		touch();
	}

	@Override
	public boolean getSetting(IslandSetting setting) {
		return settings.getOrDefault(setting, setting.getDefaultValue());
	}

	public void setSetting(IslandSetting setting, boolean value) {
		settings.put(setting, value);
		touch();
	}

	@Override
	public IslandRole getRoleOf(UUID playerUuid) {
		if (ownerUuid.equals(playerUuid)) {
			return IslandRole.OWNER;
		}

		return members.stream()
				.filter(member -> member.playerUuid().equals(playerUuid))
				.map(IslandMember::role)
				.findFirst()
				.orElse(IslandRole.VISITOR);
	}

	@Override
	public boolean hasPermission(UUID playerUuid, IslandPermission permission) {
		IslandRole role = getRoleOf(playerUuid);
		if (role == IslandRole.OWNER) {
			return true;
		}

		boolean roleDefault = defaultPermission(role, permission);

		Optional<IslandMember> member = members.stream()
				.filter(candidate -> candidate.playerUuid().equals(playerUuid))
				.findFirst();

		// An override flips the role's default for that specific permission.
		if (member.isPresent() && member.get().overrides().contains(permission)) {
			return !roleDefault;
		}

		return roleDefault;
	}

	// TODO: this default permission table will become configurable in a future sprint.
	private static boolean defaultPermission(IslandRole role, IslandPermission permission) {
		return switch (role) {
			case OWNER -> true;
			case MEMBER, TRUSTED -> permission != IslandPermission.REDSTONE;
			case VISITOR, DENIED -> false;
		};
	}

	@Override
	public Instant getCreatedAt() {
		return createdAt;
	}

	@Override
	public Instant getUpdatedAt() {
		return updatedAt;
	}

	private void touch() {
		this.updatedAt = Instant.now();
	}
}
