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
import java.util.Map;
import java.util.Optional;

// Loaded from config/islandcore/flag_permission_requirements.json on SERVER_STARTED. Maps a flag
// id to an optional LuckPerms node (checked via PERMISSION_PROVIDER) a player must hold before
// they can change that flag for their own island, via /island flags set, /island flags preset, or
// the network equivalents (FlagSetC2S/FlagSetPresetC2S) — see IslandActionService#updateFlag/
// applyFlagPreset. A flag absent from this map has no extra restriction: today's behavior,
// unchanged. Ships with natural_mob_spawning/raids pre-populated (see writeDefault) if the file
// doesn't exist yet; an operator manages it from then on via "/island admin flags require <flag>
// <nodo|ninguno>" or AdminFlagSetRequirementC2S.
public class FlagPermissionRequirements {

	private static final String CONFIG_FILE_NAME = "flag_permission_requirements.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Map<String, String> requirements = new HashMap<>();

	public FlagPermissionRequirements() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> load());
	}

	private void load() {
		Path configFile = configFile();

		if (!Files.exists(configFile)) {
			writeDefault(configFile);
		}

		requirements.clear();

		try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			for (String flagId : root.keySet()) {
				requirements.put(flagId, root.get(flagId).getAsString());
			}
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load {}, using defaults", CONFIG_FILE_NAME, e);
		}
	}

	// natural_mob_spawning and raids default to requiring their own dedicated node — see point 17
	// of the sprint that introduced this. Every other flag (including the 2 other new ISLAND_GLOBAL
	// flags, crop_trample/explosion_damage) starts with no requirement, same as before this class
	// existed.
	private void writeDefault(Path configFile) {
		JsonObject root = new JsonObject();
		root.addProperty("natural_mob_spawning", "islandcore.flags.natural_mob_spawning");
		root.addProperty("raids", "islandcore.flags.raids");

		try {
			Files.createDirectories(configFile.getParent());
			Files.writeString(configFile, GSON.toJson(root));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
		}
	}

	public Optional<String> getRequiredPermission(String flagId) {
		return Optional.ofNullable(requirements.get(flagId));
	}

	// node == null clears the requirement for this flag.
	public void setRequiredPermission(String flagId, String node) {
		if (node == null) {
			requirements.remove(flagId);
		} else {
			requirements.put(flagId, node);
		}
		persist();
	}

	private void persist() {
		JsonObject root = new JsonObject();
		for (Map.Entry<String, String> entry : requirements.entrySet()) {
			root.addProperty(entry.getKey(), entry.getValue());
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
