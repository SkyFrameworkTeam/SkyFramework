package com.skyframework.islandcore.protection.exception;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.island.model.IslandRole;
import com.skyframework.islandcore.protection.flag.FlagPreset;
import com.skyframework.islandcore.protection.flag.TriState;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Given an island, the acting player, an identifier (a block's or entity type's registry id), and
// the matching category, finds the FIRST group enabled for that PLAYER'S ROLE on that island (see
// isEnabledForRole's resolution chain: isla -> servidor -> código, exactly mirroring
// FlagResolver.resolveForRole for ROLE_BASED flags) whose pattern matches, in registration order.
// Groups not enabled for that role are skipped entirely — never even pattern-matched.
public final class ExceptionResolver {

	private ExceptionResolver() {
	}

	public static Optional<ExceptionGroup> resolve(Island island, UUID playerUuid, Identifier objectId, ExceptionGroupCategory category) {
		List<ExceptionGroup> groups = IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getGroupsForCategory(category);
		IslandRole role = island.getRoleOf(playerUuid);

		for (ExceptionGroup group : groups) {
			if (!isEnabledForRole(island, role, group)) {
				continue;
			}
			if (matchesAny(group, objectId, category)) {
				return Optional.of(group);
			}
		}

		return Optional.empty();
	}

	// Resolution order (highest priority first): isla -> servidor -> código — exactly
	// FlagResolver.resolveForRole's chain, reused here for exception groups. OWNER/DENIED never get
	// an island or server override (FlagPreset#toRoleValues only ever covers VISITOR/ALLY/MEMBER/
	// TRUSTED, same as every other preset in this codebase) and the group's own compiled
	// defaultPreset doesn't cover them either, so this always resolves to "not enabled" for those two
	// roles — which is correct: OWNER already has full access via FlagResolver's own OWNER
	// short-circuit regardless of any exception group, and DENIED must never be able to bypass an
	// explicit per-player block through a group exception.
	public static boolean isEnabledForRole(Island island, IslandRole role, ExceptionGroup group) {
		TriState islandOverride = island.getRoleExceptionGroupOverride(group.getId(), role);
		if (islandOverride != TriState.DEFAULT) {
			return islandOverride.toBoolean();
		}

		Optional<TriState> serverDefault = IslandCoreMod.SERVER_EXCEPTION_DEFAULTS.getDefault(group.getId(), role);
		if (serverDefault.isPresent()) {
			return serverDefault.get().toBoolean();
		}

		TriState codeDefault = group.getDefaultPreset().toRoleValues().get(role);
		return codeDefault != null && codeDefault.toBoolean();
	}

	// Server-layer-only resolution (skips the island-override layer entirely — there's no specific
	// island in this context): what a BRAND NEW island's role would resolve to today, given only the
	// server default and the group's own compiled defaultPreset. Used by AdminDefaultsBuilder.
	public static boolean isEnabledForRoleServerDefault(IslandRole role, ExceptionGroup group) {
		Optional<TriState> serverDefault = IslandCoreMod.SERVER_EXCEPTION_DEFAULTS.getDefault(group.getId(), role);
		if (serverDefault.isPresent()) {
			return serverDefault.get().toBoolean();
		}

		TriState codeDefault = group.getDefaultPreset().toRoleValues().get(role);
		return codeDefault != null && codeDefault.toBoolean();
	}

	// The VISITOR/ALLY/MEMBER/TRUSTED combination currently resolved for this group on this island,
	// expressed as a preset id ("nadie"/"miembros"/"aliados"/"todos"), or "custom" if it doesn't
	// exactly match any of the 4 — mirrors FlagPreset#matching's role for flags. Used by both
	// "/island exceptions list" and FlagsStatusBuilder's network status, so text and network always
	// report the exact same value.
	public static String currentPreset(Island island, ExceptionGroup group) {
		boolean visitor = isEnabledForRole(island, IslandRole.VISITOR, group);
		boolean ally = isEnabledForRole(island, IslandRole.ALLY, group);
		boolean member = isEnabledForRole(island, IslandRole.MEMBER, group);
		boolean trusted = isEnabledForRole(island, IslandRole.TRUSTED, group);
		return FlagPreset.matching(visitor, ally, member, trusted).map(FlagPreset::getId).orElse("custom");
	}

	private static boolean matchesAny(ExceptionGroup group, Identifier objectId, ExceptionGroupCategory category) {
		for (PatternMatcher matcher : group.getCompiledPatterns()) {
			if (matcher.isTagReference()) {
				if (matchesTag(matcher.getTagPath(), objectId, category)) {
					return true;
				}
			} else if (matcher.matchesLiteral(objectId.toString())) {
				return true;
			}
		}
		return false;
	}

	private static boolean matchesTag(String tagPath, Identifier objectId, ExceptionGroupCategory category) {
		Identifier tagId = Identifier.of(tagPath);

		if (category == ExceptionGroupCategory.BLOCK) {
			Block block = Registries.BLOCK.get(objectId);
			TagKey<Block> tagKey = TagKey.of(RegistryKeys.BLOCK, tagId);
			return block.getRegistryEntry().isIn(tagKey);
		}

		EntityType<?> entityType = Registries.ENTITY_TYPE.get(objectId);
		TagKey<EntityType<?>> tagKey = TagKey.of(RegistryKeys.ENTITY_TYPE, tagId);
		return entityType.getRegistryEntry().isIn(tagKey);
	}
}
