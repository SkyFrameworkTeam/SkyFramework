package com.skyframework.islandcore.party.storage;

import com.skyframework.islandcore.party.model.PartyData;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PartyStorage {

	void save(PartyData party);

	void delete(UUID partyId);

	Optional<PartyData> load(UUID partyId);

	Collection<PartyData> loadAll();
}
