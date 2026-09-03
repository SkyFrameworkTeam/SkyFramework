package com.skyframework.islandcore.protection.flag;

import com.skyframework.islandcore.island.model.IslandRole;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Every flag this sprint introduces, registered once at class-load time. The ROLE_BASED flags'
// per-role default table reproduces exactly what IslandData.hasPermission's own (now unused for
// these six) defaultPermission() switch hardcoded: OWNER always ALLOW, MEMBER/TRUSTED ALLOW except
// REDSTONE (DENY), VISITOR/DENIED always DENY — see FlagResolver for why preserving this table
// verbatim keeps today's observable behavior unchanged when nothing overrides it.
public final class FlagRegistry {

	private static final Map<String, Flag> FLAGS = new LinkedHashMap<>();

	// Merges the old separate "build"/"break" flags into one: placing and breaking blocks are the
	// same trust decision in practice (an owner who lets a member build almost always also lets
	// them break what they placed), and having them as two flags/two rows in the UI without a clear
	// distinction was confusing more than it helped. Same default table "build" had.
	public static final Flag CONSTRUCCION = registerRoleBased("construccion", false);
	public static final Flag INTERACT = registerRoleBased("interact", false);
	public static final Flag CONTAINERS = registerRoleBased("containers", false);
	public static final Flag ENTITIES = registerRoleBased("entities", false);
	public static final Flag REDSTONE = registerRoleBased("redstone", true);

	public static final Flag FIRE_SPREAD = registerGlobal("fire_spread");
	public static final Flag PVP_DAMAGE = registerGlobal("pvp_damage");
	public static final Flag MOB_DAMAGE = registerGlobal("mob_damage");
	public static final Flag EXPLOSION_DAMAGE = registerGlobal("explosion_damage");

	private FlagRegistry() {
	}

	private static Flag registerRoleBased(String id, boolean denyMemberAndTrusted) {
		Map<IslandRole, TriState> table = new EnumMap<>(IslandRole.class);
		table.put(IslandRole.OWNER, TriState.ALLOW);
		table.put(IslandRole.MEMBER, denyMemberAndTrusted ? TriState.DENY : TriState.ALLOW);
		table.put(IslandRole.TRUSTED, denyMemberAndTrusted ? TriState.DENY : TriState.ALLOW);
		// Same default as VISITOR: fully denied until the island owner explicitly opens this flag
		// for ALLY via /island flags set <flag> ally allow.
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
