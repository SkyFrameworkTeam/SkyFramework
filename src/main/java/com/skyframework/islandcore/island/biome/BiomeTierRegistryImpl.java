package com.skyframework.islandcore.island.biome;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.permission.PermissionProvider;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Loaded from config/islandcore/biome_tiers.json on SERVER_STARTED, the same trigger point
// previously used by RestrictedBiomeRegistry (Sprint 13, now retired) and by IslandRegistryImpl's
// own storage load.
public class BiomeTierRegistryImpl implements BiomeTierRegistry {

	private static final String CONFIG_FILE_NAME = "biome_tiers.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final List<BiomeTier> tiers = new ArrayList<>();

	public BiomeTierRegistryImpl() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> load());
	}

	private void load() {
		Path configFile = FabricLoader.getInstance().getConfigDir().resolve("islandcore").resolve(CONFIG_FILE_NAME);

		if (!Files.exists(configFile)) {
			writeDefault(configFile);
		}

		try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray tiersJson = root.getAsJsonArray("tiers");
			if (tiersJson == null) {
				return;
			}

			for (JsonElement tierElement : tiersJson) {
				try {
					tiers.add(parseTier(tierElement.getAsJsonObject()));
				} catch (RuntimeException e) {
					IslandCoreMod.LOGGER.error("Skipping invalid biome tier in {}: {}", CONFIG_FILE_NAME, tierElement, e);
				}
			}
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load {}, no biome tiers will be available", CONFIG_FILE_NAME, e);
		}
	}

	private static BiomeTier parseTier(JsonObject tierJson) {
		String id = tierJson.get("id").getAsString();

		JsonElement permissionElement = tierJson.get("permission");
		String permission = (permissionElement == null || permissionElement.isJsonNull())
				? null
				: permissionElement.getAsString();

		Set<Identifier> biomes = new LinkedHashSet<>();
		for (JsonElement biomeElement : tierJson.getAsJsonArray("biomes")) {
			biomes.add(Identifier.of(biomeElement.getAsString()));
		}

		return new BiomeTier(id, permission, biomes);
	}

	private void writeDefault(Path configFile) {
		JsonArray tiersArray = new JsonArray();
		tiersArray.add(defaultTierJson("base", null,
				"minecraft:plains", "minecraft:desert", "minecraft:dark_forest", "minecraft:the_void",
				"minecraft:forest", "minecraft:savanna", "minecraft:snowy_plains", "minecraft:beach"));
		tiersArray.add(defaultTierJson("adventurer", "islandcore.biome.tier.adventurer",
				"minecraft:swamp", "minecraft:jungle", "minecraft:badlands", "minecraft:taiga",
				"minecraft:snowy_taiga", "minecraft:mushroom_fields", "minecraft:ice_spikes"));
		tiersArray.add(defaultTierJson("legendary", "islandcore.biome.tier.legendary",
				"minecraft:cherry_grove", "minecraft:mangrove_swamp", "minecraft:lush_caves",
				"minecraft:dripstone_caves", "minecraft:deep_dark", "minecraft:warped_forest",
				"minecraft:crimson_forest", "minecraft:soul_sand_valley"));

		JsonObject root = new JsonObject();
		root.add("tiers", tiersArray);

		try {
			Files.createDirectories(configFile.getParent());
			Files.writeString(configFile, GSON.toJson(root));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
		}
	}

	private static JsonObject defaultTierJson(String id, String permission, String... biomes) {
		JsonObject tier = new JsonObject();
		tier.addProperty("id", id);
		tier.add("permission", permission == null ? JsonNull.INSTANCE : new JsonPrimitive(permission));

		JsonArray biomesArray = new JsonArray();
		for (String biome : biomes) {
			biomesArray.add(biome);
		}
		tier.add("biomes", biomesArray);

		return tier;
	}

	@Override
	public boolean canUse(UUID playerUuid, Identifier biomeId, PermissionProvider permissionProvider) {
		List<BiomeTier> matching = getTiersContaining(biomeId);
		if (matching.isEmpty()) {
			return false;
		}

		for (BiomeTier tier : matching) {
			if (tier.permission() == null) {
				return true;
			}
		}

		for (BiomeTier tier : matching) {
			if (permissionProvider.hasPermission(playerUuid, tier.permission())) {
				return true;
			}
		}

		return false;
	}

	@Override
	public List<BiomeTier> getTiersContaining(Identifier biomeId) {
		List<BiomeTier> result = new ArrayList<>();
		for (BiomeTier tier : tiers) {
			if (tier.biomes().contains(biomeId)) {
				result.add(tier);
			}
		}
		return result;
	}

	@Override
	public Collection<Identifier> getAvailableBiomes(UUID playerUuid, PermissionProvider permissionProvider) {
		Set<Identifier> available = new LinkedHashSet<>();
		for (BiomeTier tier : tiers) {
			if (tier.permission() == null || permissionProvider.hasPermission(playerUuid, tier.permission())) {
				available.addAll(tier.biomes());
			}
		}
		return available;
	}

	@Override
	public List<BiomeTier> getAllTiers() {
		return List.copyOf(tiers);
	}
}
