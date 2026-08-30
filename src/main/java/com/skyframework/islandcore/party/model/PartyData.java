package com.skyframework.islandcore.party.model;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

// Mutable, managed exclusively by PartyRegistry(Impl) — same pattern as IslandData/DimensionData.
public class PartyData {

	private final UUID partyId;
	private String name;
	private UUID leaderUuid;

	// Insertion order preserved deliberately: PartyRegistryImpl#leaveParty picks the oldest
	// remaining member (the next one after the leader in this set's iteration order) as the new
	// leader when the current leader leaves a party that still has other members.
	private final Set<UUID> members = new LinkedHashSet<>();

	// Unidirectional: a party declaring another as an ally doesn't require that party to accept
	// anything back. Two mutually-allying parties simply both call "ally add" on each other.
	private final Set<UUID> alliedPartyIds = new LinkedHashSet<>();

	private final Instant createdAt;
	private Instant updatedAt;

	public PartyData(UUID partyId, String name, UUID leaderUuid, Instant createdAt) {
		this.partyId = partyId;
		this.name = name;
		this.leaderUuid = leaderUuid;
		this.members.add(leaderUuid);
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	// Reconstruction constructor used when loading persisted parties from storage: unlike the
	// primary constructor, updatedAt/members/alliedPartyIds are not freshly initialized but restored as-is.
	public PartyData(
			UUID partyId,
			String name,
			UUID leaderUuid,
			Collection<UUID> members,
			Collection<UUID> alliedPartyIds,
			Instant createdAt,
			Instant updatedAt
	) {
		this.partyId = partyId;
		this.name = name;
		this.leaderUuid = leaderUuid;
		this.members.addAll(members);
		this.alliedPartyIds.addAll(alliedPartyIds);
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public UUID getPartyId() {
		return partyId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		touch();
	}

	public UUID getLeaderUuid() {
		return leaderUuid;
	}

	public void setLeaderUuid(UUID leaderUuid) {
		this.leaderUuid = leaderUuid;
		touch();
	}

	public Set<UUID> getMembers() {
		return Collections.unmodifiableSet(members);
	}

	public void addMember(UUID playerUuid) {
		members.add(playerUuid);
		touch();
	}

	public void removeMember(UUID playerUuid) {
		members.remove(playerUuid);
		touch();
	}

	public Set<UUID> getAlliedPartyIds() {
		return Collections.unmodifiableSet(alliedPartyIds);
	}

	public void addAlly(UUID targetPartyId) {
		alliedPartyIds.add(targetPartyId);
		touch();
	}

	public void removeAlly(UUID targetPartyId) {
		alliedPartyIds.remove(targetPartyId);
		touch();
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	private void touch() {
		this.updatedAt = Instant.now();
	}
}
