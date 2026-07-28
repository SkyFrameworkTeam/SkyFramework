package com.skyframework.islandcore.storage;

import com.skyframework.islandcore.island.model.IslandData;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IslandStorage {

	void save(IslandData island);

	void delete(UUID islandId);

	Optional<IslandData> load(UUID islandId);

	Collection<IslandData> loadAll();
}
