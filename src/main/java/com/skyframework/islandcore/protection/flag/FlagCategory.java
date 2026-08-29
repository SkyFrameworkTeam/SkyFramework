package com.skyframework.islandcore.protection.flag;

public enum FlagCategory {
	// Resolved per-actor via their IslandRole (build, break, interact, containers, entities, redstone).
	ROLE_BASED,

	// A single island-wide toggle, no role involved (fire_spread, pvp_damage, mob_damage).
	ISLAND_GLOBAL
}
