package com.skyframework.islandcore.net.admin.defaults;

import com.skyframework.islandcore.IslandCoreMod;
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
import java.util.List;
import java.util.Locale;

// Pure assembly, mirroring FlagsStatusBuilder: every field comes from
// FlagResolver#resolveServerDefaultForRole / ExceptionResolver#isEnabledForRoleServerDefault — the
// server-layer-only resolution (skips any specific island), reverse-matched into a preset id via
// FlagPreset#matching exactly like FlagsStatusBuilder does for an island's own resolved flags.
public final class AdminDefaultsBuilder {

	private AdminDefaultsBuilder() {
	}

	public static AdminDefaultsStatusS2C build() {
		List<AdminDefaultsStatusS2C.FlagDefaultEntry> flagDefaults = new ArrayList<>();
		for (Flag flag : FlagRegistry.all()) {
			if (flag.getCategory() != FlagCategory.ROLE_BASED) {
				continue;
			}

			boolean visitor = FlagResolver.resolveServerDefaultForRole(IslandRole.VISITOR, flag).toBoolean();
			boolean ally = FlagResolver.resolveServerDefaultForRole(IslandRole.ALLY, flag).toBoolean();
			boolean member = FlagResolver.resolveServerDefaultForRole(IslandRole.MEMBER, flag).toBoolean();
			String currentPreset = FlagPreset.matching(visitor, ally, member).map(FlagPreset::getId).orElse("custom");

			flagDefaults.add(new AdminDefaultsStatusS2C.FlagDefaultEntry(flag.getId(), currentPreset));
		}

		List<AdminDefaultsStatusS2C.ExceptionDefaultEntry> exceptionDefaults = new ArrayList<>();
		for (ExceptionGroup group : IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getAllGroups()) {
			boolean visitor = ExceptionResolver.isEnabledForRoleServerDefault(IslandRole.VISITOR, group);
			boolean ally = ExceptionResolver.isEnabledForRoleServerDefault(IslandRole.ALLY, group);
			boolean member = ExceptionResolver.isEnabledForRoleServerDefault(IslandRole.MEMBER, group);
			String currentPreset = FlagPreset.matching(visitor, ally, member).map(FlagPreset::getId).orElse("custom");

			exceptionDefaults.add(new AdminDefaultsStatusS2C.ExceptionDefaultEntry(group.getId(), currentPreset));
		}

		List<AdminDefaultsStatusS2C.GlobalDefaultEntry> globalDefaults = new ArrayList<>();
		for (Flag flag : FlagRegistry.all()) {
			if (flag.getCategory() != FlagCategory.ISLAND_GLOBAL) {
				continue;
			}

			TriState currentValue = IslandCoreMod.SERVER_FLAG_DEFAULTS.getGlobalDefault(flag.getId()).orElse(TriState.DEFAULT);
			globalDefaults.add(new AdminDefaultsStatusS2C.GlobalDefaultEntry(flag.getId(), currentValue.name().toLowerCase(Locale.ROOT)));
		}

		return new AdminDefaultsStatusS2C(flagDefaults, exceptionDefaults, globalDefaults);
	}
}
