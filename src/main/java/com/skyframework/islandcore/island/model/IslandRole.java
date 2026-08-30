package com.skyframework.islandcore.island.model;

// Enum declaration order does NOT imply hierarchy in code (nothing does ordinal comparisons on
// this enum). The real permission hierarchy, from strongest to weakest, is:
//   OWNER > DENIED (an explicit block always wins) > TRUSTED > MEMBER > ALLY > VISITOR
// ALLY sits between MEMBER and VISITOR: granted either by an explicit per-player
// "/island ally add" entry, or implicitly when the player's party is one of the island owner's
// party's declared allies (see IslandData#getRoleOf and party/ for the party system). See
// FlagResolver/FlagRegistry for how each ROLE_BASED flag's default table treats ALLY (same as
// VISITOR: fully denied until the island owner opens it explicitly).
public enum IslandRole {
	OWNER,
	MEMBER,
	TRUSTED,
	ALLY,
	VISITOR,
	DENIED
}
