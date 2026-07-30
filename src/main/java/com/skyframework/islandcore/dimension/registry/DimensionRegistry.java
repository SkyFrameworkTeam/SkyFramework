package com.skyframework.islandcore.dimension.registry;

import com.skyframework.islandcore.dimension.model.DimensionDefinition;
import com.skyframework.islandcore.dimension.model.DimensionGeneratorStyle;

import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface DimensionRegistry {

	Optional<DimensionDefinition> getDimension(Identifier id);

	Collection<DimensionDefinition> getAllDimensions();

	boolean exists(Identifier id);

	// Registers the dimension's metadata/intent and materializes it immediately (no restart needed).
	DimensionDefinition createDimension(Identifier id, String displayName, DimensionGeneratorStyle style, long seed);

	// Two-step confirmation, same pattern as IslandDeletionService.requestDeletion/confirmDeletion.
	void requestDeletion(Identifier id, UUID requestedBy);

	boolean confirmDeletion(Identifier id, UUID requestedBy);

	void requestRegeneration(Identifier id, UUID requestedBy, long newSeed);

	boolean confirmRegeneration(Identifier id, UUID requestedBy);

	// Executes the deletion immediately, bypassing the request/confirm flow. Called by
	// confirmDeletion(...) and by initializeStorage(...) to resume a deletion interrupted by a
	// previous shutdown (state == DELETING on load). Must be idempotent.
	void deleteDimension(Identifier id);

	// Executes the regeneration immediately, bypassing the request/confirm flow. Called by
	// confirmRegeneration(...) and by initializeStorage(...) to resume a regeneration interrupted
	// by a previous shutdown (state == REGENERATING on load). Must be idempotent.
	void regenerateDimension(Identifier id, long newSeed);
}
