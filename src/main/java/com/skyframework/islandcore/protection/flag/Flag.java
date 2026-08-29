package com.skyframework.islandcore.protection.flag;

import com.skyframework.islandcore.island.model.IslandRole;

import java.util.Map;

// Immutable definition of one flag: its id, category, and the hardcoded ("código") default value(s)
// FlagResolver falls back to once no island/server override applies. Instances only ever come from
// FlagRegistry — see that class for the actual registration of every flag this sprint introduces.
public final class Flag {

	private final String id;
	private final FlagCategory category;
	// ROLE_BASED only: one hardcoded TriState per IslandRole. Never null when category == ROLE_BASED.
	private final Map<IslandRole, TriState> roleDefaults;
	// ISLAND_GLOBAL only. Never null when category == ISLAND_GLOBAL.
	private final TriState globalDefault;

	private Flag(String id, FlagCategory category, Map<IslandRole, TriState> roleDefaults, TriState globalDefault) {
		this.id = id;
		this.category = category;
		this.roleDefaults = roleDefaults;
		this.globalDefault = globalDefault;
	}

	public static Flag roleBased(String id, Map<IslandRole, TriState> roleDefaults) {
		return new Flag(id, FlagCategory.ROLE_BASED, Map.copyOf(roleDefaults), null);
	}

	public static Flag islandGlobal(String id, TriState globalDefault) {
		return new Flag(id, FlagCategory.ISLAND_GLOBAL, null, globalDefault);
	}

	public String getId() {
		return id;
	}

	public FlagCategory getCategory() {
		return category;
	}

	// ROLE_BASED flags only.
	public TriState codeDefaultForRole(IslandRole role) {
		return roleDefaults.get(role);
	}

	// ISLAND_GLOBAL flags only.
	public TriState codeDefaultGlobal() {
		return globalDefault;
	}
}
