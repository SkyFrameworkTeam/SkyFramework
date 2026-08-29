package com.skyframework.islandcore.protection.exception;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

// Given an island, an identifier (a block's or entity type's registry id), and the matching
// category, finds the FIRST group enabled for that island (island override if set, else
// group.isDefaultEnabled()) whose pattern matches, in registration order. Groups the island hasn't
// enabled are skipped entirely — never even pattern-matched.
public final class ExceptionResolver {

	private ExceptionResolver() {
	}

	public static Optional<ExceptionGroup> resolve(Island island, Identifier objectId, ExceptionGroupCategory category) {
		List<ExceptionGroup> groups = IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getGroupsForCategory(category);

		for (ExceptionGroup group : groups) {
			if (!isEnabledForIsland(island, group)) {
				continue;
			}
			if (matchesAny(group, objectId, category)) {
				return Optional.of(group);
			}
		}

		return Optional.empty();
	}

	private static boolean isEnabledForIsland(Island island, ExceptionGroup group) {
		Boolean override = island.getExceptionGroupOverride(group.getId());
		return override != null ? override : group.isDefaultEnabled();
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
