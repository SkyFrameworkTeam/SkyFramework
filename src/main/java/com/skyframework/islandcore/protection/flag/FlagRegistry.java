package com.skyframework.islandcore.protection.flag;

import com.skyframework.islandcore.island.model.IslandRole;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Every flag this sprint introduces, registered once at class-load time. Every ROLE_BASED flag's
// compiled ("código") default table is now uniform: OWNER always ALLOW, MEMBER/ALLY/VISITOR/DENIED
// always DENY until the island owner explicitly opens it via a preset — CO_OWNER is deliberately
// NOT in this table at all, since it's a hard-coded always-ALLOW role FlagResolver short-circuits
// before ever consulting it (see IslandRole's javadoc).
//
// "redstone" (the ROLE_BASED flag) was retired: evidence check before removal — grep across
// AccessControllerImpl/protection listeners found no canBreak/canPlace/canInteractBlock/
// canOpenContainer/canInteractEntity call ever passed FlagRegistry.REDSTONE, so it was registered,
// listable, and configurable but never actually enforced. Placing/breaking redstone components was
// already covered by CONSTRUCCION; activating them (levers, buttons) is covered by INTERACT, with
// the separate "redstone"/"mechanisms" EXCEPTION GROUPS (still present, unrelated to this flag)
// providing the opt-in bypass for otherwise-denied roles — see ExceptionGroupRegistry.
public final class FlagRegistry {

	private static final Map<String, Flag> FLAGS = new LinkedHashMap<>();

	// Merges the old separate "build"/"break" flags into one: placing and breaking blocks are the
	// same trust decision in practice (an owner who lets a member build almost always also lets
	// them break what they placed), and having them as two flags/two rows in the UI without a clear
	// distinction was confusing more than it helped.
	public static final Flag CONSTRUCCION = registerRoleBased("construccion");
	public static final Flag INTERACT = registerRoleBased("interact");
	public static final Flag ENTITIES = registerRoleBased("entities");

	public static final Flag FIRE_SPREAD = registerGlobal("fire_spread");
	public static final Flag PVP_DAMAGE = registerGlobal("pvp_damage");
	public static final Flag MOB_DAMAGE = registerGlobal("mob_damage");
	public static final Flag EXPLOSION_DAMAGE = registerGlobal("explosion_damage");
	public static final Flag CROP_TRAMPLE = registerGlobal("crop_trample");
	// Scoped to hostile (SpawnGroup.MONSTER) natural spawns only — see SpawnHelperMixin — despite
	// the generic-sounding id, matching the investigation this implements (natural HOSTILE mob
	// spawning specifically), not passive animals/villagers.
	public static final Flag NATURAL_MOB_SPAWNING = registerGlobal("natural_mob_spawning");
	public static final Flag RAIDS = registerGlobal("raids");

	private FlagRegistry() {
	}

	private static Flag registerRoleBased(String id) {
		Map<IslandRole, TriState> table = new EnumMap<>(IslandRole.class);
		table.put(IslandRole.OWNER, TriState.ALLOW);
		// MEMBER now shares ALLY/VISITOR's compiled default: fully denied until the island owner
		// explicitly opens this flag via a preset ("/island flags preset <flag> miembros" or
		// higher) — no more special-cased ALLOW-by-default for MEMBER.
		table.put(IslandRole.MEMBER, TriState.DENY);
		table.put(IslandRole.ALLY, TriState.DENY);
		table.put(IslandRole.VISITOR, TriState.DENY);
		table.put(IslandRole.DENIED, TriState.DENY);

		Flag flag = Flag.roleBased(id, table);
		FLAGS.put(id, flag);
		return flag;
	}

	// Default DENY for every ISLAND_GLOBAL flag, matching fire_spread/pvp_damage/mob_damage's
	// existing IslandSetting default (false) exactly.
	private static Flag registerGlobal(String id) {
		Flag flag = Flag.islandGlobal(id, TriState.DENY);
		FLAGS.put(id, flag);
		return flag;
	}

	public static Optional<Flag> get(String id) {
		return Optional.ofNullable(FLAGS.get(id));
	}

	public static List<Flag> all() {
		return List.copyOf(FLAGS.values());
	}
}
