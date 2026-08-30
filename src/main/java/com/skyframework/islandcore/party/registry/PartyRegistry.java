package com.skyframework.islandcore.party.registry;

import com.skyframework.islandcore.party.model.PartyData;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PartyRegistry {

	Optional<PartyData> getParty(UUID partyId);

	// Searches every loaded party for one that has this player as a member (the leader counts as
	// a member too — see PartyData's constructor). Empty if the player isn't in any party.
	Optional<PartyData> getPartyOf(UUID playerUuid);

	// Case-insensitive lookup, used to resolve "/party ally add <nombre>" and the name-uniqueness
	// check in createParty/renameParty.
	Optional<PartyData> getPartyByName(String name);

	Collection<PartyData> getAllParties();

	// Throws IllegalStateException if playerUuid is already in a party (a player can only be in one
	// party at a time, mirroring IslandRegistryImpl#createIslandInternal's one-island-per-owner rule)
	// or if the name is already taken (case-insensitive).
	PartyData createParty(String name, UUID leaderUuid);

	// Idempotent no-op if the party doesn't exist.
	void disbandParty(UUID partyId);

	// Throws IllegalStateException if playerUuid is already in a (different) party.
	void addMember(UUID partyId, UUID playerUuid);

	void removeMember(UUID partyId, UUID playerUuid);

	void addAlly(UUID partyId, UUID targetPartyId);

	void removeAlly(UUID partyId, UUID targetPartyId);

	// Throws IllegalStateException if the new name is already taken (case-insensitive) by another party.
	void renameParty(UUID partyId, String newName);

	// Leaves playerUuid's current party. If they're the leader and other members remain, leadership
	// transfers to the oldest remaining member (insertion order); if they're the leader and the only
	// member, the party is disbanded instead. Returns true if the party was disbanded as a result,
	// false otherwise (including if the player wasn't in a party at all). See PartyRegistryImpl for
	// the full reasoning.
	boolean leaveParty(UUID playerUuid);

	void initializeStorage(Path worldSaveDir);

	// Saves every currently loaded party; used as an extra safety net on server shutdown
	// (individual mutations already persist themselves), mirroring IslandRegistryApi#saveAll.
	void saveAll();
}
