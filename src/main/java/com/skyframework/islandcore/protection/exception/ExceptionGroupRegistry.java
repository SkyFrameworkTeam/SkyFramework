package com.skyframework.islandcore.protection.exception;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.protection.flag.FlagPreset;

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

// Loaded from config/islandcore/exception_groups.json on SERVER_STARTED. Ships with 10 example
// groups (doors, chests, redstone, animals, crops, furnaces, mechanisms, bells, lecterns, beds),
// all off (preset "nadie") by default, if the file doesn't exist yet. This file is the "código"
// layer of the exception resolution chain (see
// ExceptionResolver) — an admin edits it by hand to change a group's compiled default preset;
// runtime tuning happens one layer up via ServerExceptionDefaults ("/island admin exceptions
// set-default"), which is why this registry no longer exposes any in-game mutator.
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
		FlagPreset defaultPreset = readDefaultPreset(json);
		boolean ownerConfigurable = json.get("ownerConfigurable").getAsBoolean();

		return new ExceptionGroup(id, category, patterns, allowInteract, allowBreak, requireEmptyHand, defaultPreset, ownerConfigurable);
	}

	// Tolerant of the pre-role-based schema: if the new "defaultPreset" field (nadie/miembros/
	// aliados/todos) is present, use it directly; otherwise translate the old "defaultEnabled"
	// boolean this field replaced — true meant "on for everyone" (exactly the "todos" preset: every
	// role's resolved value is true), false meant "off for everyone" ("nadie") — a lossless,
	// behavior-preserving one-time translation, not an approximation.
	private static FlagPreset readDefaultPreset(JsonObject json) {
		if (json.has("defaultPreset")) {
			String raw = json.get("defaultPreset").getAsString();
			return FlagPreset.fromId(raw)
					.orElseThrow(() -> new IllegalArgumentException("defaultPreset desconocido: " + raw));
		}
		boolean legacyDefaultEnabled = json.get("defaultEnabled").getAsBoolean();
		return legacyDefaultEnabled ? FlagPreset.EVERYONE : FlagPreset.NOBODY;
	}

	private void writeDefault(Path configFile) {
		// Lowercase ids (as of this sprint): easier to type in commands, and /island exceptions set
		// now normalizes its own "group" argument to lowercase too (see IslandCommand), so this is
		// the canonical case going forward.
		List<ExceptionGroup> defaults = List.of(
				new ExceptionGroup("doors", ExceptionGroupCategory.BLOCK,
						List.of("#minecraft:doors", "#minecraft:trapdoors", "#minecraft:fence_gates"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("chests", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel", "#minecraft:shulker_boxes"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("redstone", ExceptionGroupCategory.BLOCK,
						List.of("#minecraft:buttons", "minecraft:lever"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("animals", ExceptionGroupCategory.ENTITY,
						List.of("minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:cat", "minecraft:wolf", "minecraft:parrot"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("crops", ExceptionGroupCategory.BLOCK,
						List.of("#minecraft:crops"),
						false, true, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("furnaces", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:furnace", "minecraft:smoker", "minecraft:blast_furnace"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("mechanisms", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:lever", "minecraft:*_button"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("bells", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:bell"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("lecterns", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:lectern"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("beds", ExceptionGroupCategory.BLOCK,
						List.of("#minecraft:beds"),
						true, false, false, FlagPreset.NOBODY, true)
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
		json.addProperty("defaultPreset", group.getDefaultPreset().getId());
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

	private static Path configFile() {
		return FabricLoader.getInstance().getConfigDir().resolve("islandcore").resolve(CONFIG_FILE_NAME);
	}
}
