package com.skyframework.islandcore.dimension.vanilla;

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
import java.util.Locale;

// Loaded from config/islandcore/vanilla_reset.json on SERVER_STARTED, same pattern as RtpConfig.
//
// "seedMode" controls what VanillaResetService.confirmReset() does when the admin doesn't give an
// explicit seed:
//  - "new" (default, and the fallback for any unrecognized/missing value): roll a fresh random seed.
//  - "keep": leave the seed untouched (persist seed=null, meaning "don't rewrite level.dat's seed").
public class VanillaResetConfig {

	private static final String CONFIG_FILE_NAME = "vanilla_reset.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private String seedMode = "new";

	public VanillaResetConfig() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> load());
	}

	private void load() {
		Path configFile = FabricLoader.getInstance().getConfigDir().resolve("islandcore").resolve(CONFIG_FILE_NAME);

		if (!Files.exists(configFile)) {
			writeDefault(configFile);
		}

		try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

			if (root.has("seedMode")) {
				String mode = root.get("seedMode").getAsString().toLowerCase(Locale.ROOT);
				if (mode.equals("new") || mode.equals("keep")) {
					seedMode = mode;
				} else {
					IslandCoreMod.LOGGER.warn("Unrecognized seedMode \"{}\" in {}, defaulting to \"new\"", mode, CONFIG_FILE_NAME);
					seedMode = "new";
				}
			}
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load {}, using defaults", CONFIG_FILE_NAME, e);
		}
	}

	private void writeDefault(Path configFile) {
		JsonObject root = new JsonObject();
		root.addProperty("seedMode", "new");

		try {
			Files.createDirectories(configFile.getParent());
			Files.writeString(configFile, GSON.toJson(root));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
		}
	}

	public String getSeedMode() {
		return seedMode;
	}
}
