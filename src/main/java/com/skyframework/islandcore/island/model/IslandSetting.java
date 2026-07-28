package com.skyframework.islandcore.island.model;

// Grows with more per-island settings in future sprints. Each entry defines its own safe
// default value.
public enum IslandSetting {
	FIRE_SPREAD(false),

	// Damage strictly between two players.
	PVP_DAMAGE(false),

	// Any damage where at least one side (attacker or victim) is NOT a player: protects the
	// island's animals/villagers/mobs, and protects players from hostile mobs.
	MOB_DAMAGE(false);

	// Neither PVP_DAMAGE nor MOB_DAMAGE applies to damage with no attacker
	// (falls, drowning, lava, starvation, etc.).

	private final boolean defaultValue;

	IslandSetting(boolean defaultValue) {
		this.defaultValue = defaultValue;
	}

	public boolean getDefaultValue() {
		return defaultValue;
	}
}
