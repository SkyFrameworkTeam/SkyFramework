package com.skyframework.islandcore.protection.exception;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.island.model.IslandRole;
import com.skyframework.islandcore.protection.flag.FlagPreset;
import com.skyframework.islandcore.protection.flag.TriState;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// Loaded from config/islandcore/exception_defaults.json on SERVER_STARTED, exact same pattern as
// ServerFlagDefaults — the "servidor" layer in the exception resolution chain (isla -> servidor ->
// código), between an island's own per-role override and the group's own compiled defaultPreset
// (see ExceptionResolver). Only groups an admin has explicitly set via /island admin exceptions
// set-default appear here; every exception group is role-based (no ISLAND_GLOBAL-style equivalent),
// so unlike ServerFlagDefaults this only ever needs the one preset-shaped map.
public class ServerExceptionDefaults {

	private static final String CONFIG_FILE_NAME = "exception_defaults.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Map<String, FlagPreset> overrides = new HashMap<>();

	public ServerExceptionDefaults() {
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
			for (String groupId : root.keySet()) {
				String raw = root.get(groupId).getAsString();
				Optional<FlagPreset> preset = FlagPreset.fromId(raw);
				if (preset.isPresent()) {
					overrides.put(groupId, preset.get());
				} else {
					IslandCoreMod.LOGGER.error("Skipping invalid preset \"{}\" for exception group \"{}\" in {}", raw, groupId, CONFIG_FILE_NAME);
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

	// Per-role: empty falls through to the group's own compiled defaultPreset — see
	// ExceptionResolver.
	public Optional<TriState> getDefault(String groupId, IslandRole role) {
		FlagPreset preset = overrides.get(groupId);
		if (preset == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(preset.toRoleValues().get(role));
	}

	// For reporting the current server-wide preset (AdminDefaultsStatusS2C).
	public Optional<FlagPreset> getPreset(String groupId) {
		return Optional.ofNullable(overrides.get(groupId));
	}

	// preset == null clears the server-wide default for this group.
	public void setDefault(String groupId, FlagPreset preset) {
		if (preset == null) {
			overrides.remove(groupId);
		} else {
			overrides.put(groupId, preset);
		}
		persist();
	}

	private void persist() {
		JsonObject root = new JsonObject();
		for (Map.Entry<String, FlagPreset> entry : overrides.entrySet()) {
			root.addProperty(entry.getKey(), entry.getValue().getId());
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
