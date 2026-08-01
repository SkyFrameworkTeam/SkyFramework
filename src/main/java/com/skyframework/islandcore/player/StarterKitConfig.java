package com.skyframework.islandcore.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.skyframework.islandcore.IslandCoreMod;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Loaded from config/islandcore/starter_kit.json on SERVER_STARTED, same pattern as
// SpawnConfig/RtpConfig/FarmingConfig.
public class StarterKitConfig {

	private static final String CONFIG_FILE_NAME = "starter_kit.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final List<ItemStackDefinition> items = new ArrayList<>();

	public StarterKitConfig() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> load());
	}

	private void load() {
		Path configFile = FabricLoader.getInstance().getConfigDir().resolve("islandcore").resolve(CONFIG_FILE_NAME);

		if (!Files.exists(configFile)) {
			writeDefault(configFile);
		}

		items.clear();
		try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray itemsArray = root.getAsJsonArray("items");
			if (itemsArray == null) {
				return;
			}

			for (JsonElement itemElement : itemsArray) {
				try {
					JsonObject itemObject = itemElement.getAsJsonObject();
					String item = itemObject.get("item").getAsString();
					int count = itemObject.get("count").getAsInt();
					items.add(new ItemStackDefinition(item, count));
				} catch (RuntimeException e) {
					IslandCoreMod.LOGGER.error("Skipping invalid starter kit entry in {}: {}", CONFIG_FILE_NAME, itemElement, e);
				}
			}
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load {}, no starter kit will be given", CONFIG_FILE_NAME, e);
		}
	}

	private void writeDefault(Path configFile) {
		JsonObject oakLog = new JsonObject();
		oakLog.addProperty("item", "minecraft:oak_log");
		oakLog.addProperty("count", 64);

		JsonObject flintAndSteel = new JsonObject();
		flintAndSteel.addProperty("item", "minecraft:flint_and_steel");
		flintAndSteel.addProperty("count", 1);

		JsonArray itemsArray = new JsonArray();
		itemsArray.add(oakLog);
		itemsArray.add(flintAndSteel);

		JsonObject root = new JsonObject();
		root.add("items", itemsArray);

		try {
			Files.createDirectories(configFile.getParent());
			Files.writeString(configFile, GSON.toJson(root));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
		}
	}

	public List<ItemStackDefinition> getItems() {
		return List.copyOf(items);
	}

	public record ItemStackDefinition(String item, int count) {
	}
}
