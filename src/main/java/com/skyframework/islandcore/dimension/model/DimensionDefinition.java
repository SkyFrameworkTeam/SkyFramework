package com.skyframework.islandcore.dimension.model;

import net.minecraft.util.Identifier;

import java.time.Instant;

public interface DimensionDefinition {

	// The dimension's real technical identifier, e.g. "islandcore:farming".
	Identifier getId();

	// Cosmetic label shown in chat messages, independent of the technical id.
	String getDisplayName();

	DimensionGeneratorStyle getGeneratorStyle();

	long getSeed();

	DimensionState getState();

	Instant getCreatedAt();

	Instant getUpdatedAt();
}
