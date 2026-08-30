package com.skyframework.islandcore.party.registry;

import com.skyframework.islandcore.party.model.PartyData;
import com.skyframework.islandcore.party.storage.NbtPartyStorage;
import com.skyframework.islandcore.party.storage.PartyStorage;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Deliberately independent of the island/ package, same registry+storage shape as
// DimensionRegistryImpl: self-contained SERVER_STARTED hook, one NBT file per party.
public class PartyRegistryImpl implements PartyRegistry {

	private final Map<UUID, PartyData> partiesById = new LinkedHashMap<>();

	private PartyStorage storage;

	public PartyRegistryImpl() {
		ServerLifecycleEvents.SERVER_STARTED.register(startedServer ->
				initializeStorage(startedServer.getSavePath(WorldSavePath.ROOT)));
	}

	@Override
	public void initializeStorage(Path worldSaveDir) {
		storage = new NbtPartyStorage(worldSaveDir.resolve("islandcore").resolve("parties"));

		for (PartyData party : storage.loadAll()) {
			partiesById.put(party.getPartyId(), party);
		}
	}

	@Override
	public Optional<PartyData> getParty(UUID partyId) {
		return Optional.ofNullable(partiesById.get(partyId));
	}

	@Override
	public Optional<PartyData> getPartyOf(UUID playerUuid) {
		for (PartyData party : partiesById.values()) {
			if (party.getMembers().contains(playerUuid)) {
				return Optional.of(party);
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<PartyData> getPartyByName(String name) {
		for (PartyData party : partiesById.values()) {
			if (party.getName().equalsIgnoreCase(name)) {
				return Optional.of(party);
			}
		}
		return Optional.empty();
	}

	@Override
	public Collection<PartyData> getAllParties() {
		return List.copyOf(partiesById.values());
	}

	@Override
	public PartyData createParty(String name, UUID leaderUuid) {
		if (getPartyOf(leaderUuid).isPresent()) {
			throw new IllegalStateException("Ya perteneces a una party. Sal de ella con /party leave antes de crear otra.");
		}
		if (getPartyByName(name).isPresent()) {
			throw new IllegalStateException("Ya existe una party con el nombre \"" + name + "\".");
		}

		UUID partyId = UUID.randomUUID();
		PartyData party = new PartyData(partyId, name, leaderUuid, Instant.now());

		partiesById.put(partyId, party);
		saveIfStorageReady(party);

		return party;
	}

	@Override
	public void disbandParty(UUID partyId) {
		PartyData party = partiesById.remove(partyId);
		if (party != null && storage != null) {
			storage.delete(partyId);
		}
	}

	@Override
	public void addMember(UUID partyId, UUID playerUuid) {
		PartyData party = partiesById.get(partyId);
		if (party == null) {
			return;
		}

		Optional<PartyData> existing = getPartyOf(playerUuid);
		if (existing.isPresent() && !existing.get().getPartyId().equals(partyId)) {
			throw new IllegalStateException("Ese jugador ya pertenece a otra party.");
		}

		party.addMember(playerUuid);
		saveIfStorageReady(party);
	}

	@Override
	public void removeMember(UUID partyId, UUID playerUuid) {
		PartyData party = partiesById.get(partyId);
		if (party != null) {
			party.removeMember(playerUuid);
			saveIfStorageReady(party);
		}
	}

	@Override
	public void addAlly(UUID partyId, UUID targetPartyId) {
		PartyData party = partiesById.get(partyId);
		if (party != null) {
			party.addAlly(targetPartyId);
			saveIfStorageReady(party);
		}
	}

	@Override
	public void removeAlly(UUID partyId, UUID targetPartyId) {
		PartyData party = partiesById.get(partyId);
		if (party != null) {
			party.removeAlly(targetPartyId);
			saveIfStorageReady(party);
		}
	}

	@Override
	public void renameParty(UUID partyId, String newName) {
		PartyData party = partiesById.get(partyId);
		if (party == null) {
			return;
		}

		Optional<PartyData> existing = getPartyByName(newName);
		if (existing.isPresent() && !existing.get().getPartyId().equals(partyId)) {
			throw new IllegalStateException("Ya existe una party con el nombre \"" + newName + "\".");
		}

		party.setName(newName);
		saveIfStorageReady(party);
	}

	// Leave policy (no existing precedent to mirror — a deliberate, documented judgment call, as
	// requested by the sprint brief): a non-leader simply leaves, no side effects on the party
	// itself. A leader leaving a party they're not alone in transfers leadership to the oldest
	// remaining member (the next one in PartyData#getMembers()'s insertion-order iteration once the
	// leaving leader is skipped) rather than forcing them to kick everyone first — that mirrors how
	// real party/guild systems in other games commonly behave and avoids a leader being unable to
	// leave without first dismantling the whole party. A leader leaving a party they're the only
	// member of disbands it instead, since an empty, leaderless party would otherwise persist forever.
	@Override
	public boolean leaveParty(UUID playerUuid) {
		Optional<PartyData> maybeParty = getPartyOf(playerUuid);
		if (maybeParty.isEmpty()) {
			return false;
		}
		PartyData party = maybeParty.get();

		if (!party.getLeaderUuid().equals(playerUuid)) {
			party.removeMember(playerUuid);
			saveIfStorageReady(party);
			return false;
		}

		if (party.getMembers().size() <= 1) {
			disbandParty(party.getPartyId());
			return true;
		}

		Iterator<UUID> it = party.getMembers().iterator();
		UUID nextLeader = null;
		while (it.hasNext()) {
			UUID member = it.next();
			if (!member.equals(playerUuid)) {
				nextLeader = member;
				break;
			}
		}

		party.removeMember(playerUuid);
		party.setLeaderUuid(nextLeader);
		saveIfStorageReady(party);
		return false;
	}

	@Override
	public void saveAll() {
		if (storage == null) {
			return;
		}
		for (PartyData party : partiesById.values()) {
			storage.save(party);
		}
	}

	private void saveIfStorageReady(PartyData party) {
		if (storage != null) {
			storage.save(party);
		}
	}
}
