package com.skyframework.islandcore.api.permission;

// Placeholder permission node constants for functionality not implemented yet.
// This list will grow as each corresponding feature is actually built in future sprints.
public final class IslandPermissions {

	public static final String ISLAND_CREATE = "islandcore.island.create";

	// Opposite of a normal "positive" permission: by default (no LuckPerms, or this node not
	// granted) EVERY player can create an island. Granting this node true to a specific player
	// or group explicitly blocks island creation for them, without affecting anyone else.
	// Intended for admins who need to restrict creation on a case-by-case basis.
	public static final String ISLAND_CREATE_DENY = "islandcore.island.create.deny";

	public static final String ISLAND_TRUST = "islandcore.island.trust";
	public static final String TELEPORT_HOME = "islandcore.teleport.home";
	public static final String TELEPORT_COOLDOWN_BYPASS = "islandcore.teleport.cooldown.bypass";

	// The full node is ISLAND_SIZE_NODE_PREFIX + size, e.g. "islandcore.island.size.25".
	public static final String ISLAND_SIZE_NODE_PREFIX = "islandcore.island.size.";

	// Size granted to any player with no island size node granted at all.
	public static final int DEFAULT_ISLAND_SIZE = 15;

	// The full node is TELEPORT_COOLDOWN_NODE_PREFIX + seconds, e.g. "islandcore.teleport.cooldown.120".
	// Unlike ISLAND_SIZE_NODE_PREFIX (where the highest granted value wins), here the LOWEST
	// granted value wins: fewer seconds of waiting is better for the player. Independent of,
	// and lower priority than, TELEPORT_COOLDOWN_BYPASS, which still skips the cooldown entirely
	// regardless of any tier node granted here.
	public static final String TELEPORT_COOLDOWN_NODE_PREFIX = "islandcore.teleport.cooldown.";

	// Cooldown applied to any player with no cooldown tier node granted at all.
	public static final int DEFAULT_HOME_COOLDOWN_SECONDS = 600;

	private IslandPermissions() {
	}
}
