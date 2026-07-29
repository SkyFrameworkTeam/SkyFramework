package com.skyframework.islandcore.dimension.registry;

import com.skyframework.islandcore.dimension.model.DimensionDefinition;
import com.skyframework.islandcore.dimension.model.DimensionGeneratorStyle;

import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Optional;

public interface DimensionRegistry {

	Optional<DimensionDefinition> getDimension(Identifier id);

	Collection<DimensionDefinition> getAllDimensions();

	boolean exists(Identifier id);

	// Registers the dimension's metadata/intent only. Does not materialize a real ServerWorld yet
	// (see Sprint 16). Throws IllegalStateException if id already exists.
	DimensionDefinition createDimension(Identifier id, String displayName, DimensionGeneratorStyle style, long seed);

	// Not implemented yet: see Sprint 17.
	void deleteDimension(Identifier id);

	// Not implemented yet: see Sprint 17.
	void regenerateDimension(Identifier id, long newSeed);
}
