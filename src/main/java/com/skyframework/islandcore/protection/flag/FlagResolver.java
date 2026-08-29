package com.skyframework.islandcore.protection.flag;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.island.IslandPermission;
import com.skyframework.islandcore.island.model.IslandRole;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

// Resolution order (highest priority first): miembro -> isla -> servidor -> código.
//  - miembro: the pre-existing per-player IslandMember.overrides() mechanism (unchanged since
//    before this flag system existed) flips whatever the isla/servidor/código chain resolves to,
//    for that one player only.
//  - isla: this island's own per-flag override (Island#getRoleFlagOverride/getGlobalFlagOverride),
//    settable by the owner via /island flags set — the owner can override ANY flag, including
//    ROLE_BASED ones, for their own island.
//  - servidor: ServerFlagDefaults, settable by an admin via /island admin flags set-default.
//  - código: the Flag's own hardcoded table (see FlagRegistry).
// The island owner, as an ACTOR, always resolves ALLOW unconditionally (matches
// IslandData.hasPermission's pre-existing "if (role == OWNER) return true" short-circuit) — separate
// from the owner's ability to CONFIGURE flags above, which isla/servidor/código don't affect.
public final class FlagResolver {

	private FlagResolver() {
	}

	// ROLE_BASED flags only.
	public static boolean resolveForPlayer(Island island, UUID playerUuid, Flag flag) {
		IslandRole role = island.getRoleOf(playerUuid);
		if (role == IslandRole.OWNER) {
			return true;
		}

		boolean base = resolveForRole(island, role, flag).toBoolean();

		if (hasMemberOverride(island, playerUuid, flag)) {
			return !base;
		}
		return base;
	}

	// ROLE_BASED flags only. Never returns TriState.DEFAULT: bottoms out at the flag's own
	// hardcoded per-role default if nothing overrides it.
	public static TriState resolveForRole(Island island, IslandRole role, Flag flag) {
		TriState islandOverride = island.getRoleFlagOverride(flag.getId(), role);
		if (islandOverride != TriState.DEFAULT) {
			return islandOverride;
		}

		Optional<TriState> serverDefault = IslandCoreMod.SERVER_FLAG_DEFAULTS.getDefault(flag.getId());
		if (serverDefault.isPresent()) {
			return serverDefault.get();
		}

		return flag.codeDefaultForRole(role);
	}

	// ISLAND_GLOBAL flags only.
	public static boolean resolveGlobal(Island island, Flag flag) {
		TriState islandOverride = island.getGlobalFlagOverride(flag.getId());
		if (islandOverride != TriState.DEFAULT) {
			return islandOverride.toBoolean();
		}

		Optional<TriState> serverDefault = IslandCoreMod.SERVER_FLAG_DEFAULTS.getDefault(flag.getId());
		if (serverDefault.isPresent()) {
			return serverDefault.get().toBoolean();
		}

		return flag.codeDefaultGlobal().toBoolean();
	}

	// Reuses the exact pre-existing per-member override mechanism (IslandMember.overrides(), a
	// Set<IslandPermission>) rather than introducing a second, competing one: ROLE_BASED flag ids
	// are deliberately the lowercase IslandPermission names, so the mapping back is exact.
	private static boolean hasMemberOverride(Island island, UUID playerUuid, Flag flag) {
		IslandPermission permission = toIslandPermission(flag);

		return island.getMembers().stream()
				.filter(member -> member.playerUuid().equals(playerUuid))
				.findFirst()
				.map(member -> member.overrides().contains(permission))
				.orElse(false);
	}

	private static IslandPermission toIslandPermission(Flag flag) {
		return IslandPermission.valueOf(flag.getId().toUpperCase(Locale.ROOT));
	}
}
