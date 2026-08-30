package com.skyframework.islandcore.island.model;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.island.IslandPermission;
import com.skyframework.islandcore.api.island.IslandState;
import com.skyframework.islandcore.party.model.PartyData;
import com.skyframework.islandcore.protection.flag.TriState;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
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
	private final Map<String, TriState> globalFlagOverrides = new HashMap<>();
	private final Map<String, Map<IslandRole, TriState>> roleFlagOverrides = new HashMap<>();
	private final Map<String, Boolean> exceptionGroupOverrides = new HashMap<>();

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
			String currentBiomeId,
			Map<String, TriState> globalFlagOverrides,
			Map<String, Map<IslandRole, TriState>> roleFlagOverrides,
			Map<String, Boolean> exceptionGroupOverrides
	) {
		this(islandId, ownerUuid, dimension, gridX, gridZ, center, bounds, plotBounds,
				islandSize, plotSize, islandType, homeLocation, state, createdAt, currentBiomeId);
		this.updatedAt = updatedAt;
		this.members.addAll(members);
		this.settings.putAll(settings);
		this.lastBiomeChangeAt = lastBiomeChangeAt;
		this.globalFlagOverrides.putAll(globalFlagOverrides);
		this.roleFlagOverrides.putAll(roleFlagOverrides);
		this.exceptionGroupOverrides.putAll(exceptionGroupOverrides);
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
	public TriState getGlobalFlagOverride(String flagId) {
		return globalFlagOverrides.getOrDefault(flagId, TriState.DEFAULT);
	}

	public void setGlobalFlagOverride(String flagId, TriState value) {
		if (value == TriState.DEFAULT) {
			globalFlagOverrides.remove(flagId);
		} else {
			globalFlagOverrides.put(flagId, value);
		}
		touch();
	}

	@Override
	public TriState getRoleFlagOverride(String flagId, IslandRole role) {
		Map<IslandRole, TriState> perRole = roleFlagOverrides.get(flagId);
		if (perRole == null) {
			return TriState.DEFAULT;
		}
		return perRole.getOrDefault(role, TriState.DEFAULT);
	}

	// /island flags set has no per-role argument (only <flag> <value>): it overrides every role
	// uniformly. The richer per-role map shape is kept in storage regardless, so a future sprint
	// could expose a finer-grained command without a schema change.
	public void setRoleFlagOverrideForAllRoles(String flagId, TriState value) {
		if (value == TriState.DEFAULT) {
			roleFlagOverrides.remove(flagId);
		} else {
			Map<IslandRole, TriState> perRole = new EnumMap<>(IslandRole.class);
			for (IslandRole role : IslandRole.values()) {
				perRole.put(role, value);
			}
			roleFlagOverrides.put(flagId, perRole);
		}
		touch();
	}

	@Override
	public Boolean getExceptionGroupOverride(String groupId) {
		return exceptionGroupOverrides.get(groupId);
	}

	public void setExceptionGroupOverride(String groupId, Boolean value) {
		if (value == null) {
			exceptionGroupOverrides.remove(groupId);
		} else {
			exceptionGroupOverrides.put(groupId, value);
		}
		touch();
	}

	// Raw-map accessors for NbtIslandStorage only (mirrors getMembers()'s read-only-view pattern):
	// unlike getSetting()'s per-key resolved-value approach, these overrides need to be persisted
	// SPARSELY (only what this island actually overrides), since their resolution also depends on
	// server-level defaults NbtIslandStorage/IslandData know nothing about.
	public Map<String, TriState> getGlobalFlagOverrides() {
		return Collections.unmodifiableMap(globalFlagOverrides);
	}

	public Map<String, Map<IslandRole, TriState>> getRoleFlagOverrides() {
		return Collections.unmodifiableMap(roleFlagOverrides);
	}

	public Map<String, Boolean> getExceptionGroupOverrides() {
		return Collections.unmodifiableMap(exceptionGroupOverrides);
	}

	@Override
	public IslandRole getRoleOf(UUID playerUuid) {
		if (ownerUuid.equals(playerUuid)) {
			return IslandRole.OWNER;
		}

		Optional<IslandMember> explicit = members.stream()
				.filter(member -> member.playerUuid().equals(playerUuid))
				.findFirst();

		// An explicit DENIED/TRUSTED/MEMBER entry always wins over party-derived roles below,
		// matching the real hierarchy documented on IslandRole (OWNER > DENIED > TRUSTED > MEMBER >
		// ALLY > VISITOR): the owner's explicit block or trust decision about one specific player
		// must never be silently overridden by that player's party membership. ALLY is deliberately
		// the one explicit role checked AFTER the party-MEMBER lookup: party membership should
		// upgrade an explicit ALLY entry to MEMBER, not the other way around.
		if (explicit.isPresent()) {
			IslandRole role = explicit.get().role();
			if (role == IslandRole.DENIED || role == IslandRole.TRUSTED || role == IslandRole.MEMBER) {
				return role;
			}
		}

		// The one justified cross-package lookup from island/ into party/ — same justification
		// pattern as DimensionRegistryImpl's single read-only lookup into island/ for Spawn
		// evacuation (see that class's "Eviction note"). Only ever reads PartyRegistry, never
		// mutates it.
		Optional<PartyData> ownerParty = IslandCoreMod.PARTY_REGISTRY.getPartyOf(ownerUuid);
		Optional<PartyData> playerParty = IslandCoreMod.PARTY_REGISTRY.getPartyOf(playerUuid);

		if (ownerParty.isPresent() && playerParty.isPresent()
				&& ownerParty.get().getPartyId().equals(playerParty.get().getPartyId())) {
			return IslandRole.MEMBER;
		}

		if (explicit.isPresent() && explicit.get().role() == IslandRole.ALLY) {
			return IslandRole.ALLY;
		}

		if (ownerParty.isPresent() && playerParty.isPresent()
				&& ownerParty.get().getAlliedPartyIds().contains(playerParty.get().getPartyId())) {
			return IslandRole.ALLY;
		}

		return IslandRole.VISITOR;
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
			case ALLY, VISITOR, DENIED -> false;
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
