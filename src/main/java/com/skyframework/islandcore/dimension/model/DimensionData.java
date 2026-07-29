package com.skyframework.islandcore.dimension.model;

import net.minecraft.util.Identifier;

import java.time.Instant;

// Mutable, managed exclusively by DimensionRegistry(Impl) — same pattern as IslandData.
public class DimensionData implements DimensionDefinition {

	private final Identifier id;
	private final String displayName;
	private final DimensionGeneratorStyle generatorStyle;
	private final long seed;
	private DimensionState state;

	private final Instant createdAt;
	private Instant updatedAt;

	public DimensionData(
			Identifier id,
			String displayName,
			DimensionGeneratorStyle generatorStyle,
			long seed,
			DimensionState state,
			Instant createdAt,
			Instant updatedAt
	) {
		this.id = id;
		this.displayName = displayName;
		this.generatorStyle = generatorStyle;
		this.seed = seed;
		this.state = state;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	@Override
	public Identifier getId() {
		return id;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public DimensionGeneratorStyle getGeneratorStyle() {
		return generatorStyle;
	}

	@Override
	public long getSeed() {
		return seed;
	}

	@Override
	public DimensionState getState() {
		return state;
	}

	public void setState(DimensionState state) {
		this.state = state;
		touch();
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
