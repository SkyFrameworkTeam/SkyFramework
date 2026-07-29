package com.skyframework.islandcore.dimension.storage;

import com.skyframework.islandcore.dimension.model.DimensionData;

import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Optional;

public interface DimensionStorage {

	void save(DimensionData dimension);

	void delete(Identifier dimensionId);

	Optional<DimensionData> load(Identifier dimensionId);

	Collection<DimensionData> loadAll();
}
