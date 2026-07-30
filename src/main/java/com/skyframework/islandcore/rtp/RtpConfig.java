package com.skyframework.islandcore.rtp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.skyframework.islandcore.IslandCoreMod;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

// Loaded from config/islandcore/rtp.json on SERVER_STARTED, same pattern as BiomeTierRegistryImpl.
//
// "mode" controls how the "dimensions" list is interpreted:
//  - "blacklist" (default, and the fallback for any unrecognized/missing mode value): every
//    dimension is allowed EXCEPT the ones listed. E.g. all dimensions but islandcore:islands.
//  - "whitelist": only the dimensions listed are allowed. E.g. ["minecraft:overworld",
//    "minecraft:the_nether"] allows /rtp only in those two; an empty list under "whitelist" means
//    /rtp is disabled everywhere.
public class RtpConfig {

	private static final String CONFIG_FILE_NAME = "rtp.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private boolean enabled = true;
	private int radiusMin = 100;
	private int radiusMax = 5000;
	private int maxAttempts = 30;
	private boolean whitelist = false;
	private final Set<Identifier> dimensions = new HashSet<>();

	public RtpConfig() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> load());
	}

	private void load() {
		Path configFile = FabricLoader.getInstance().getConfigDir().resolve("islandcore").resolve(CONFIG_FILE_NAME);

		if (!Files.exists(configFile)) {
			writeDefault(configFile);
		}

		try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

			// Tolerant to schema: "enabled" was added after this file could already exist from an
			// earlier testing session, so a missing key just keeps the enabled=true default.
			if (root.has("enabled")) {
				enabled = root.get("enabled").getAsBoolean();
			}
			if (root.has("radiusMin")) {
				radiusMin = root.get("radiusMin").getAsInt();
			}
			if (root.has("radiusMax")) {
				radiusMax = root.get("radiusMax").getAsInt();
			}
			if (root.has("maxAttempts")) {
				maxAttempts = root.get("maxAttempts").getAsInt();
			}

			String mode = root.has("mode") ? root.get("mode").getAsString() : "blacklist";
			whitelist = "whitelist".equalsIgnoreCase(mode);

			JsonArray dimensionsArray = root.getAsJsonArray("dimensions");
			if (dimensionsArray != null) {
				for (JsonElement element : dimensionsArray) {
					try {
						dimensions.add(Identifier.of(element.getAsString()));
					} catch (RuntimeException e) {
						IslandCoreMod.LOGGER.error("Skipping invalid dimension id in {}: {}", CONFIG_FILE_NAME, element, e);
					}
				}
			}
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load {}, using defaults", CONFIG_FILE_NAME, e);
		}
	}

	private void writeDefault(Path configFile) {
		JsonObject root = new JsonObject();
		root.addProperty("enabled", true);
		root.addProperty("radiusMin", 100);
		root.addProperty("radiusMax", 5000);
		root.addProperty("maxAttempts", 30);
		root.addProperty("mode", "blacklist");

		JsonArray dimensionsArray = new JsonArray();
		dimensionsArray.add("islandcore:islands");
		root.add("dimensions", dimensionsArray);

		try {
			Files.createDirectories(configFile.getParent());
			Files.writeString(configFile, GSON.toJson(root));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
		}
	}

	public int getRadiusMin() {
		return radiusMin;
	}

	public int getRadiusMax() {
		return radiusMax;
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isAllowed(Identifier dimensionId) {
		boolean listed = dimensions.contains(dimensionId);
		return whitelist == listed;
	}
}
