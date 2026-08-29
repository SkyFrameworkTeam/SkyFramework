package com.skyframework.islandcore.protection.exception;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// Loaded from config/islandcore/exception_groups.json on SERVER_STARTED. Ships with 4 example
// groups (Doors, Chests, Redstone, Animals), all off by default, if the file doesn't exist yet.
public class ExceptionGroupRegistry {

	private static final String CONFIG_FILE_NAME = "exception_groups.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Map<String, ExceptionGroup> groupsById = new LinkedHashMap<>();

	public ExceptionGroupRegistry() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> load());
	}

	private void load() {
		Path configFile = configFile();

		if (!Files.exists(configFile)) {
			writeDefault(configFile);
		}

		groupsById.clear();

		try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
			for (JsonElement element : array) {
				try {
					ExceptionGroup group = fromJson(element.getAsJsonObject());
					groupsById.put(group.getId(), group);
				} catch (RuntimeException e) {
					IslandCoreMod.LOGGER.error("Skipping malformed exception group entry in {}", CONFIG_FILE_NAME, e);
				}
			}
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load {}, no exception groups available", CONFIG_FILE_NAME, e);
		}
	}

	private static ExceptionGroup fromJson(JsonObject json) {
		String id = json.get("id").getAsString();
		ExceptionGroupCategory category =
				ExceptionGroupCategory.valueOf(json.get("category").getAsString().toUpperCase(Locale.ROOT));

		List<String> patterns = new ArrayList<>();
		for (JsonElement pattern : json.getAsJsonArray("patterns")) {
			patterns.add(pattern.getAsString());
		}

		boolean allowInteract = json.get("allowInteract").getAsBoolean();
		boolean allowBreak = json.get("allowBreak").getAsBoolean();
		boolean requireEmptyHand = json.get("requireEmptyHand").getAsBoolean();
		boolean defaultEnabled = json.get("defaultEnabled").getAsBoolean();
		boolean ownerConfigurable = json.get("ownerConfigurable").getAsBoolean();

		return new ExceptionGroup(id, category, patterns, allowInteract, allowBreak, requireEmptyHand, defaultEnabled, ownerConfigurable);
	}

	private void writeDefault(Path configFile) {
		List<ExceptionGroup> defaults = List.of(
				new ExceptionGroup("Doors", ExceptionGroupCategory.BLOCK,
						List.of("#minecraft:doors", "#minecraft:trapdoors", "#minecraft:fence_gates"),
						true, false, false, false, true),
				new ExceptionGroup("Chests", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel", "#minecraft:shulker_boxes"),
						true, false, false, false, true),
				new ExceptionGroup("Redstone", ExceptionGroupCategory.BLOCK,
						List.of("#minecraft:buttons", "minecraft:lever"),
						true, false, false, false, true),
				new ExceptionGroup("Animals", ExceptionGroupCategory.ENTITY,
						List.of("minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:cat", "minecraft:wolf", "minecraft:parrot"),
						true, false, false, false, true)
		);

		JsonArray array = new JsonArray();
		defaults.forEach(group -> array.add(toJson(group)));

		try {
			Files.createDirectories(configFile.getParent());
			Files.writeString(configFile, GSON.toJson(array));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
		}
	}

	private static JsonObject toJson(ExceptionGroup group) {
		JsonObject json = new JsonObject();
		json.addProperty("id", group.getId());
		json.addProperty("category", group.getCategory().name());

		JsonArray patternsArray = new JsonArray();
		group.getPatterns().forEach(patternsArray::add);
		json.add("patterns", patternsArray);

		json.addProperty("allowInteract", group.isAllowInteract());
		json.addProperty("allowBreak", group.isAllowBreak());
		json.addProperty("requireEmptyHand", group.isRequireEmptyHand());
		json.addProperty("defaultEnabled", group.isDefaultEnabled());
		json.addProperty("ownerConfigurable", group.isOwnerConfigurable());

		return json;
	}

	public List<ExceptionGroup> getGroupsForCategory(ExceptionGroupCategory category) {
		List<ExceptionGroup> result = new ArrayList<>();
		for (ExceptionGroup group : groupsById.values()) {
			if (group.getCategory() == category) {
				result.add(group);
			}
		}
		return result;
	}

	public Optional<ExceptionGroup> getGroup(String id) {
		return Optional.ofNullable(groupsById.get(id));
	}

	public List<ExceptionGroup> getAllGroups() {
		return List.copyOf(groupsById.values());
	}

	// Used by /island admin exceptions set-default: rewrites the group's server-wide defaultEnabled
	// and persists the whole file (every group is re-serialized, not just this one — ExceptionGroup
	// instances don't track their own position in the JSON array).
	public void setDefaultEnabled(String groupId, boolean value) {
		ExceptionGroup existing = groupsById.get(groupId);
		if (existing == null) {
			throw new IllegalArgumentException("No existe ningún grupo de excepción con id: " + groupId);
		}

		ExceptionGroup updated = new ExceptionGroup(
				existing.getId(), existing.getCategory(), existing.getPatterns(),
				existing.isAllowInteract(), existing.isAllowBreak(), existing.isRequireEmptyHand(),
				value, existing.isOwnerConfigurable());
		groupsById.put(groupId, updated);

		persist();
	}

	private void persist() {
		JsonArray array = new JsonArray();
		for (ExceptionGroup group : groupsById.values()) {
			array.add(toJson(group));
		}

		Path configFile = configFile();
		try {
			Files.createDirectories(configFile.getParent());
			Files.writeString(configFile, GSON.toJson(array));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to persist {}", CONFIG_FILE_NAME, e);
		}
	}

	private static Path configFile() {
		return FabricLoader.getInstance().getConfigDir().resolve("islandcore").resolve(CONFIG_FILE_NAME);
	}
}
