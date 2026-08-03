package com.skyframework.islandcore.island.model;

import java.util.Optional;

// Grows with more per-island settings in future sprints. Each entry defines its own safe
// default value.
public enum IslandSetting {
	FIRE_SPREAD("firespread", false),

	// Damage strictly between two players.
	PVP_DAMAGE("pvp", false),

	// Any damage where at least one side (attacker or victim) is NOT a player: protects the
	// island's animals/villagers/mobs, and protects players from hostile mobs.
	MOB_DAMAGE("mobdamage", false),

	// true (the default) = today's behavior unchanged: AccessControllerImpl still enforces the
	// usual role-based BUILD/BREAK check. false = anyone may build/break with no role check at
	// all — see AccessControllerImpl's check() for exactly where this short-circuits. Currently
	// only exposed for the Spawn island via /island admin spawn settings buildprotection and its
	// network equivalent, but the setting itself is generic (per-island), like every other entry
	// in this enum.
	BUILD_PROTECTION("buildprotection", true);

	// Neither PVP_DAMAGE nor MOB_DAMAGE applies to damage with no attacker
	// (falls, drowning, lava, starvation, etc.).

	private final String id;
	private final boolean defaultValue;

	IslandSetting(String id, boolean defaultValue) {
		this.id = id;
		this.defaultValue = defaultValue;
	}

	public String getId() {
		return id;
	}

	public boolean getDefaultValue() {
		return defaultValue;
	}

	// Same id strings and case-insensitive matching IslandCommand's resolveSetting() already used
	// (firespread/pvp/mobdamage), now shared so the network layer resolves settings identically
	// without duplicating the mapping.
	public static Optional<IslandSetting> fromId(String id) {
		for (IslandSetting setting : values()) {
			if (setting.id.equalsIgnoreCase(id)) {
				return Optional.of(setting);
			}
		}
		return Optional.empty();
	}
}
