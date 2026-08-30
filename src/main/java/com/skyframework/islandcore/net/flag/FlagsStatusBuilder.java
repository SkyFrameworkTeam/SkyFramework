package com.skyframework.islandcore.net.flag;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.island.model.IslandRole;
import com.skyframework.islandcore.protection.exception.ExceptionGroup;
import com.skyframework.islandcore.protection.flag.Flag;
import com.skyframework.islandcore.protection.flag.FlagCategory;
import com.skyframework.islandcore.protection.flag.FlagRegistry;
import com.skyframework.islandcore.protection.flag.FlagResolver;
import com.skyframework.islandcore.protection.flag.TriState;

import java.util.ArrayList;
import java.util.List;

// Pure assembly, mirroring IslandSnapshotBuilder/AdminIslandBuilder: every field comes from
// FlagRegistry/FlagResolver/ExceptionGroupRegistry — the exact same calls executeFlagsList/
// executeExceptionsList already make — no new business logic lives here.
public final class FlagsStatusBuilder {

	private FlagsStatusBuilder() {
	}

	public static FlagsStatusS2C buildFlagsStatus(Island island) {
		List<FlagsStatusS2C.FlagEntry> entries = new ArrayList<>();

		for (Flag flag : FlagRegistry.all()) {
			if (flag.getCategory() == FlagCategory.ROLE_BASED) {
				List<FlagsStatusS2C.RoleValueEntry> resolvedByRole = new ArrayList<>();
				for (IslandRole role : IslandRole.values()) {
					TriState value = FlagResolver.resolveForRole(island, role, flag);
					resolvedByRole.add(new FlagsStatusS2C.RoleValueEntry(role.name(), value.name()));
				}
				// A ROLE_BASED override always applies to every role uniformly (see
				// IslandData#setRoleFlagOverrideForAllRoles), so any one role's override reading
				// (MEMBER, arbitrarily) represents "the island's override" for this flag as a whole.
				TriState islandOverride = island.getRoleFlagOverride(flag.getId(), IslandRole.MEMBER);
				entries.add(new FlagsStatusS2C.FlagEntry(flag.getId(), flag.getCategory().name(), false, resolvedByRole, islandOverride.name()));
			} else {
				boolean resolvedValue = FlagResolver.resolveGlobal(island, flag);
				TriState islandOverride = island.getGlobalFlagOverride(flag.getId());
				entries.add(new FlagsStatusS2C.FlagEntry(flag.getId(), flag.getCategory().name(), resolvedValue, List.of(), islandOverride.name()));
			}
		}

		return new FlagsStatusS2C(entries);
	}

	public static ExceptionGroupsStatusS2C buildExceptionGroupsStatus(Island island) {
		List<ExceptionGroupsStatusS2C.GroupEntry> entries = new ArrayList<>();

		for (ExceptionGroup group : IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getAllGroups()) {
			Boolean override = island.getExceptionGroupOverride(group.getId());
			boolean enabled = override != null ? override : group.isDefaultEnabled();
			entries.add(new ExceptionGroupsStatusS2C.GroupEntry(
					group.getId(), group.getCategory().name(), enabled, group.isOwnerConfigurable()));
		}

		return new ExceptionGroupsStatusS2C(entries);
	}
}
