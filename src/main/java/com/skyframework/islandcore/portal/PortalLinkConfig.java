package com.skyframework.islandcore.portal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.skyframework.islandcore.IslandCoreMod;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// Loaded from config/islandcore/portals.json on SERVER_STARTED, same pattern as the other
// config/islandcore/*.json files already in this project (RtpConfig, SpawnConfig, ...).
//
// Known, accepted limitation: the Nether is a single shared dimension, so it can only have ONE
// configured return destination at a time. If several of our own dimensions are each configured
// to send their portals to "minecraft:the_nether", whichever entry has "minecraft:the_nether" as
// its own key LAST in the file wins for portals lit from inside the Nether itself — there is no
// way to make the shared Nether remember which of several origins a given player actually came
// from, so only one "Nether -> X" return link can ever be active at once.
public class PortalLinkConfig {

	private static final String CONFIG_FILE_NAME = "portals.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Map<Identifier, Identifier> links = new HashMap<>();

	public PortalLinkConfig() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> load());
	}

	private void load() {
		Path configFile = FabricLoader.getInstance().getConfigDir().resolve("islandcore").resolve(CONFIG_FILE_NAME);

		if (!Files.exists(configFile)) {
			writeDefault(configFile);
		}

		try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

			for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
				try {
					Identifier from = Identifier.of(entry.getKey());
					Identifier to = Identifier.of(entry.getValue().getAsString());
					links.put(from, to);
				} catch (RuntimeException e) {
					IslandCoreMod.LOGGER.error("Skipping invalid portal link entry in {}: {}", CONFIG_FILE_NAME, entry, e);
				}
			}
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load {}, no configured portal links will be available", CONFIG_FILE_NAME, e);
		}
	}

	private void writeDefault(Path configFile) {
		JsonObject root = new JsonObject();
		root.addProperty("islandcore:islands", "minecraft:the_nether");
		root.addProperty("minecraft:the_nether", "islandcore:islands");

		try {
			Files.createDirectories(configFile.getParent());
			Files.writeString(configFile, GSON.toJson(root));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
		}
	}

	// True for minecraft:overworld/minecraft:the_nether too — those already work via vanilla and
	// must keep working exactly as before, regardless of what's configured here — plus any
	// dimension id present as a key in the config.
	public boolean isPortalAllowed(Identifier dimensionId) {
		return World.OVERWORLD.getValue().equals(dimensionId)
				|| World.NETHER.getValue().equals(dimensionId)
				|| links.containsKey(dimensionId);
	}

	public Optional<Identifier> getDestination(Identifier dimensionId) {
		return Optional.ofNullable(links.get(dimensionId));
	}
}
