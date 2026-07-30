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

	// Same criteria as TELEPORT_COOLDOWN_NODE_PREFIX above: the LOWEST granted <prefix><N> tier
	// wins, and BIOME_COOLDOWN_BYPASS (which shares this prefix) skips the cooldown entirely.
	public static final String BIOME_COOLDOWN_NODE_PREFIX = "islandcore.biome.cooldown.";
	public static final String BIOME_COOLDOWN_BYPASS = "islandcore.biome.cooldown.bypass";

	// Cooldown applied to any player with no cooldown tier node granted at all: 7 days.
	public static final long DEFAULT_BIOME_COOLDOWN_SECONDS = 604800L;

	// /rtp is a general server-scope command, not island-specific — it lives in this class only
	// because this is the shared permission-node registry, same criteria already applied above to
	// the teleport/biome cooldown nodes. Same pattern: lowest granted <prefix><N> tier wins, and
	// RTP_COOLDOWN_BYPASS (sharing this prefix) skips the cooldown entirely.
	public static final String RTP_COOLDOWN_NODE_PREFIX = "islandcore.rtp.cooldown.";
	public static final String RTP_COOLDOWN_BYPASS = "islandcore.rtp.cooldown.bypass";

	// Cooldown applied to any player with no cooldown tier node granted at all.
	public static final long DEFAULT_RTP_COOLDOWN_SECONDS = 600L;

	// /spawn is also a general server-scope command, not island-specific — same criteria as
	// RTP_COOLDOWN_* above. Same pattern: lowest granted <prefix><N> tier wins, and
	// SPAWN_COOLDOWN_BYPASS (sharing this prefix) skips the cooldown entirely.
	public static final String SPAWN_COOLDOWN_NODE_PREFIX = "islandcore.spawn.cooldown.";
	public static final String SPAWN_COOLDOWN_BYPASS = "islandcore.spawn.cooldown.bypass";

	// Cooldown applied to any player with no cooldown tier node granted at all.
	public static final long DEFAULT_SPAWN_COOLDOWN_SECONDS = 600L;

	private IslandPermissions() {
	}
}
