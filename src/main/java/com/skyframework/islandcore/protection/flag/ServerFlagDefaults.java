package com.skyframework.islandcore.protection.flag;

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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// Loaded from config/islandcore/flag_defaults.json on SERVER_STARTED, same pattern as RtpConfig.
// Empty (an empty JSON object) if the file doesn't exist yet. Only flags an admin has explicitly
// set via /island admin flags set-default appear here; anything absent falls through to that
// Flag's own hardcoded code-level default inside FlagResolver.
public class ServerFlagDefaults {

	private static final String CONFIG_FILE_NAME = "flag_defaults.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Map<String, TriState> overrides = new HashMap<>();

	public ServerFlagDefaults() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> load());
	}

	private void load() {
		Path configFile = configFile();

		if (!Files.exists(configFile)) {
			writeDefault(configFile);
			return;
		}

		overrides.clear();

		try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			for (String flagId : root.keySet()) {
				try {
					TriState value = TriState.valueOf(root.get(flagId).getAsString().toUpperCase(Locale.ROOT));
					if (value != TriState.DEFAULT) {
						overrides.put(flagId, value);
					}
				} catch (IllegalArgumentException e) {
					IslandCoreMod.LOGGER.error("Skipping invalid value for flag \"{}\" in {}", flagId, CONFIG_FILE_NAME, e);
				}
			}
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load {}, using defaults", CONFIG_FILE_NAME, e);
		}
	}

	private void writeDefault(Path configFile) {
		try {
			Files.createDirectories(configFile.getParent());
			Files.writeString(configFile, GSON.toJson(new JsonObject()));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
		}
	}

	public Optional<TriState> getDefault(String flagId) {
		return Optional.ofNullable(overrides.get(flagId));
	}

	// value == TriState.DEFAULT clears the server-wide override for this flag.
	public void setDefault(String flagId, TriState value) {
		if (value == TriState.DEFAULT) {
			overrides.remove(flagId);
		} else {
			overrides.put(flagId, value);
		}
		persist();
	}

	private void persist() {
		JsonObject root = new JsonObject();
		for (Map.Entry<String, TriState> entry : overrides.entrySet()) {
			root.addProperty(entry.getKey(), entry.getValue().name());
		}

		Path configFile = configFile();
		try {
			Files.createDirectories(configFile.getParent());
			Files.writeString(configFile, GSON.toJson(root));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to persist {}", CONFIG_FILE_NAME, e);
		}
	}

	private static Path configFile() {
		return FabricLoader.getInstance().getConfigDir().resolve("islandcore").resolve(CONFIG_FILE_NAME);
	}
}
