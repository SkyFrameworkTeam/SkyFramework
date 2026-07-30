package com.skyframework.islandcore.spawn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

// Loaded from config/islandcore/spawn.json on SERVER_STARTED, same pattern as RtpConfig.
public class SpawnConfig {

	private static final String CONFIG_FILE_NAME = "spawn.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private boolean enabled = true;

	public SpawnConfig() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> load());
	}

	private void load() {
		Path configFile = FabricLoader.getInstance().getConfigDir().resolve("islandcore").resolve(CONFIG_FILE_NAME);

		if (!Files.exists(configFile)) {
			writeDefault(configFile);
		}

		try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			if (root.has("enabled")) {
				enabled = root.get("enabled").getAsBoolean();
			}
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load {}, using defaults", CONFIG_FILE_NAME, e);
		}
	}

	private void writeDefault(Path configFile) {
		JsonObject root = new JsonObject();
		root.addProperty("enabled", true);

		try {
			Files.createDirectories(configFile.getParent());
			Files.writeString(configFile, GSON.toJson(root));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
		}
	}

	public boolean isEnabled() {
		return enabled;
	}
}
