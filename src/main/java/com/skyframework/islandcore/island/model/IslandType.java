package com.skyframework.islandcore.island.model;

// TODO: this will become a configurable, data-driven island type system in a future sprint.
public class IslandType {

	public static final IslandType PLAINS = new IslandType("plains", "Plains");

	private final String id;
	private final String displayName;

	private IslandType(String id, String displayName) {
		this.id = id;
		this.displayName = displayName;
	}

	public String getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	// Only lookup needed while PLAINS is the sole type; will be replaced by a real registry
	// lookup once island types become configurable.
	public static IslandType fromId(String id) {
		if (PLAINS.getId().equals(id)) {
			return PLAINS;
		}
		throw new IllegalArgumentException("Unknown island type id: " + id);
	}
}
