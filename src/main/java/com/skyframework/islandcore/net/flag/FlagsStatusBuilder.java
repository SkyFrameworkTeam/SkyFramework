package com.skyframework.islandcore.net.flag;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.island.model.IslandRole;
import com.skyframework.islandcore.protection.exception.ExceptionGroup;
import com.skyframework.islandcore.protection.exception.ExceptionResolver;
import com.skyframework.islandcore.protection.flag.Flag;
import com.skyframework.islandcore.protection.flag.FlagCategory;
import com.skyframework.islandcore.protection.flag.FlagPreset;
import com.skyframework.islandcore.protection.flag.FlagRegistry;
import com.skyframework.islandcore.protection.flag.FlagResolver;
import com.skyframework.islandcore.protection.flag.TriState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Pure assembly, mirroring IslandSnapshotBuilder/AdminIslandBuilder: every field comes from
// FlagRegistry/FlagResolver/ExceptionGroupRegistry — the exact same calls executeFlagsList/
// executeExceptionsList already make — no new business logic lives here.
public final class FlagsStatusBuilder {

	private FlagsStatusBuilder() {
	}

	public static FlagsStatusS2C buildFlagsStatus(Island island, UUID playerUuid) {
		List<FlagsStatusS2C.FlagEntry> entries = new ArrayList<>();

		for (Flag flag : FlagRegistry.all()) {
			boolean missingRequiredPermission = IslandCoreMod.FLAG_PERMISSION_REQUIREMENTS.getRequiredPermission(flag.getId())
					.map(node -> !IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, node))
					.orElse(false);

			if (flag.getCategory() == FlagCategory.ROLE_BASED) {
				List<FlagsStatusS2C.RoleValueEntry> resolvedByRole = new ArrayList<>();
				Map<IslandRole, Boolean> resolvedAllowByRole = new EnumMap<>(IslandRole.class);
				for (IslandRole role : IslandRole.values()) {
					TriState value = FlagResolver.resolveForRole(island, role, flag);
					resolvedByRole.add(new FlagsStatusS2C.RoleValueEntry(role.name(), value.name()));
					resolvedAllowByRole.put(role, value.toBoolean());
				}

				// islandOverride reads MEMBER's own raw override as a representative single value —
				// still meaningful for a flag set via "/island flags set" (applies to every role
				// uniformly), but no longer necessarily representative once a preset (see below) has
				// set VISITOR/ALLY/MEMBER to different values; currentPreset is the accurate signal
				// for that case.
				TriState islandOverride = island.getRoleFlagOverride(flag.getId(), IslandRole.MEMBER);

				String currentPreset = FlagPreset.matching(
								resolvedAllowByRole.get(IslandRole.VISITOR), resolvedAllowByRole.get(IslandRole.ALLY),
								resolvedAllowByRole.get(IslandRole.MEMBER))
						.map(FlagPreset::getId)
						.orElse("custom");

				entries.add(new FlagsStatusS2C.FlagEntry(
						flag.getId(), flag.getCategory().name(), false, resolvedByRole, islandOverride.name(), currentPreset, missingRequiredPermission));
			} else {
				boolean resolvedValue = FlagResolver.resolveGlobal(island, flag);
				TriState islandOverride = island.getGlobalFlagOverride(flag.getId());
				entries.add(new FlagsStatusS2C.FlagEntry(flag.getId(), flag.getCategory().name(), resolvedValue, List.of(), islandOverride.name(), "", missingRequiredPermission));
			}
		}

		return new FlagsStatusS2C(entries);
	}

	public static ExceptionGroupsStatusS2C buildExceptionGroupsStatus(Island island) {
		List<ExceptionGroupsStatusS2C.GroupEntry> entries = new ArrayList<>();

		for (ExceptionGroup group : IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getAllGroups()) {
			List<FlagsStatusS2C.RoleValueEntry> resolvedByRole = new ArrayList<>();
			for (IslandRole role : IslandRole.values()) {
				boolean enabled = ExceptionResolver.isEnabledForRole(island, role, group);
				resolvedByRole.add(new FlagsStatusS2C.RoleValueEntry(role.name(), TriState.fromBoolean(enabled).name()));
			}

			String currentPreset = ExceptionResolver.currentPreset(island, group);

			entries.add(new ExceptionGroupsStatusS2C.GroupEntry(
					group.getId(), group.getCategory().name(), resolvedByRole, currentPreset, group.isOwnerConfigurable()));
		}

		return new ExceptionGroupsStatusS2C(entries);
	}
}
