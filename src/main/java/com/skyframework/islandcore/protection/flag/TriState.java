package com.skyframework.islandcore.protection.flag;

// DEFAULT is a sentinel meaning "no explicit value at this layer, fall through to the next one" —
// FlagResolver never returns it as a final resolved value (only ALLOW/DENY are terminal).
public enum TriState {
	ALLOW,
	DENY,
	DEFAULT;

	public boolean toBoolean() {
		return this == ALLOW;
	}

	public static TriState fromBoolean(boolean value) {
		return value ? ALLOW : DENY;
	}
}
