package com.skyframework.islandcore.net.biome;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.island.biome.BiomeTier;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BiomeTiersBuilder {

	private BiomeTiersBuilder() {
	}

	public static BiomeTiersS2C build(ServerPlayerEntity player) {
		UUID playerUuid = player.getUuid();

		List<BiomeTiersS2C.TierEntry> tierEntries = IslandCoreMod.BIOME_TIER_REGISTRY.getAllTiers().stream()
				.map(tier -> toTierEntry(tier, playerUuid))
				.toList();

		return new BiomeTiersS2C(tierEntries);
	}

	private static BiomeTiersS2C.TierEntry toTierEntry(BiomeTier tier, UUID playerUuid) {
		boolean unlocked = tier.permission() == null
				|| IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, tier.permission());

		List<BiomeTiersS2C.BiomeEntry> biomeEntries = tier.biomes().stream()
				.map(biomeId -> new BiomeTiersS2C.BiomeEntry(biomeId.toString(), formatLabel(biomeId)))
				.toList();

		return new BiomeTiersS2C.TierEntry(tier.id(), Optional.ofNullable(tier.permission()), unlocked, biomeEntries);
	}

	// "minecraft:dark_forest" -> "Dark Forest". No existing label/translation mechanism for
	// biomes elsewhere in the codebase to reuse, so this is a plain, dependency-free formatter
	// rather than pulling in the client-only lang file lookup Minecraft itself uses for biome names.
	private static String formatLabel(Identifier biomeId) {
		String[] words = biomeId.getPath().split("_");
		StringBuilder label = new StringBuilder();
		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			if (!label.isEmpty()) {
				label.append(' ');
			}
			label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return label.toString();
	}
}
