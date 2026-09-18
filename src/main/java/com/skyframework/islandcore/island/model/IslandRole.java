package com.skyframework.islandcore.island.model;

// Enum declaration order does NOT imply hierarchy in code (nothing does ordinal comparisons on
// this enum). The real permission hierarchy, from strongest to weakest, is:
//   OWNER > DENIED (an explicit block always wins) > CO_OWNER > MEMBER > ALLY > VISITOR
// CO_OWNER (formerly "TRUSTED") is a hard-coded always-ALLOW role for every ROLE_BASED flag,
// exactly as strong as OWNER — see FlagResolver, which short-circuits it before consulting any
// island override, server default, or compiled table. It does NOT participate in FlagPreset's
// 4-level VISITOR/ALLY/MEMBER table: an island owner can't weaken it, only promote/demote a
// member into or out of it (see MembershipService#trust/untrust).
// ALLY sits between MEMBER and VISITOR: granted by an explicit per-player "/island alliance add"
// entry (or MemberAllyAddC2S from the client's Party menu — see MembershipService#allyAdd), managed
// by the island's OWNER or CO_OWNER. See FlagRegistry for how each ROLE_BASED flag's compiled
// default treats MEMBER/ALLY/VISITOR identically (all DENY) until the island owner opens it
// explicitly via a preset.
public enum IslandRole {
	OWNER,
	MEMBER,
	CO_OWNER,
	ALLY,
	VISITOR,
	DENIED
}
