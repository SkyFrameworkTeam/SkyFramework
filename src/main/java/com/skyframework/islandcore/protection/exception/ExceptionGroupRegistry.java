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
import java.util.Set;

// Loaded from config/islandcore/exception_groups.json on SERVER_STARTED. Ships with 20 example
// groups (doors, chests, animals, crops, furnaces, buttons, levers, bells, lecterns, beds,
// barrels, shulker_boxes, hoppers, dispensers_droppers, crafting_tables, anvils,
// enchanting_tables, jukeboxes, note_blocks, cakes), all off (preset "nadie") by default, if
// the file doesn't exist yet — see compiledDefaults(). If the file DOES already exist (e.g. from a
// server install predating one or more of these groups), load() also fills in any group present in
// compiledDefaults() but missing from the file, and rewrites it — without touching any entry
// already there, so a hand-edited group's patterns/preset/ownerConfigurable are never overwritten.
// It also removes any RETIRED_GROUP_IDS entry unconditionally (currently "redstone"/"mechanisms",
// both superseded by "buttons"/"levers" — see compiledDefaults()), since there's nothing
// customizable left to preserve for an id no longer resolved against at all.
// This file is the "código" layer of the exception resolution chain (see ExceptionResolver) — an
// admin edits it by hand to change a group's compiled default preset; runtime tuning happens one
// layer up via ServerExceptionDefaults ("/island admin exceptions set-default"), which is why this
// registry no longer exposes any in-game mutator.
public class ExceptionGroupRegistry {

	private static final String CONFIG_FILE_NAME = "exception_groups.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	// "redstone" was a duplicate of "mechanisms" (both covered the same buttons/levers);
	// "mechanisms" itself was then split into "buttons"/"levers" — see compiledDefaults(). Neither
	// id is resolved against by ExceptionResolver anymore, so an entry under either is just inert
	// dead weight left over from before this sprint, not something an admin could still be
	// meaningfully customizing.
	private static final Set<String> RETIRED_GROUP_IDS = Set.of("redstone", "mechanisms");

	private final Map<String, ExceptionGroup> groupsById = new LinkedHashMap<>();

	public ExceptionGroupRegistry() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> load());
	}

	private void load() {
		Path configFile = configFile();
		List<ExceptionGroup> defaults = compiledDefaults();

		if (!Files.exists(configFile)) {
			writeGroups(configFile, defaults);
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
			// Don't attempt the merge-and-rewrite below over a file we couldn't even parse — that
			// could clobber whatever's actually on disk with a partial/empty groupsById.
			return;
		}

		// Retired group ids (see RETIRED_GROUP_IDS) removed unconditionally, even from a file that
		// already existed — unlike a merely-missing group, there's no admin customization worth
		// preserving for an id ExceptionResolver no longer resolves against at all; leaving it in
		// would just reintroduce the exact pattern-overlap duplication this retirement fixes (e.g.
		// "mechanisms" sitting alongside its own replacements "buttons"/"levers").
		boolean removedRetired = groupsById.keySet().removeAll(RETIRED_GROUP_IDS);

		// The file already existed (possibly from before one or more of these groups were added in
		// a later sprint — see this class's javadoc) and is missing one or more code-registered
		// groups: add each missing one with its compiled default and rewrite the file. Never touches
		// an entry already present, so an admin's hand-edited pattern/preset/ownerConfigurable for an
		// existing group is preserved exactly as-is.
		List<ExceptionGroup> missing = defaults.stream().filter(group -> !groupsById.containsKey(group.getId())).toList();
		if (removedRetired || !missing.isEmpty()) {
			missing.forEach(group -> groupsById.put(group.getId(), group));
			writeGroups(configFile, List.copyOf(groupsById.values()));
			IslandCoreMod.LOGGER.info("Updated {}: removed retired group(s) {}, added missing default group(s) {}",
					CONFIG_FILE_NAME, removedRetired ? RETIRED_GROUP_IDS : Set.of(),
					missing.stream().map(ExceptionGroup::getId).toList());
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

	// Lowercase ids (as of an earlier sprint): easier to type in commands, and /island exceptions
	// set now normalizes its own "group" argument to lowercase too (see IslandCommand), so this is
	// the canonical case going forward. Used both to seed a brand-new config file and, on an
	// existing one, to fill in any group missing from it (see load()) — the single source of truth
	// for what a group's compiled ("código") default looks like, never read directly by resolution
	// logic (ExceptionResolver only ever reads groupsById, populated from the loaded/merged file).
	private static List<ExceptionGroup> compiledDefaults() {
		return List.of(
				new ExceptionGroup("doors", ExceptionGroupCategory.BLOCK,
						List.of("#minecraft:doors", "#minecraft:trapdoors", "#minecraft:fence_gates"),
						true, false, false, FlagPreset.NOBODY, true),
				// barrel/shulker_boxes deliberately NOT included here (narrowed this sprint): the
				// dedicated "barrels"/"shulker_boxes" groups below are now the only ones responsible
				// for those blocks — this group previously duplicated them, letting either group's
				// preset independently unlock the same block.
				new ExceptionGroup("chests", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:chest", "minecraft:trapped_chest"),
						true, false, false, FlagPreset.NOBODY, true),
				// "redstone" removed (was a duplicate of "mechanisms", both covering the same
				// buttons/levers — see the split below) and "mechanisms" itself split into two
				// independent groups so an owner can open buttons without also opening levers or
				// vice versa.
				new ExceptionGroup("animals", ExceptionGroupCategory.ENTITY,
						List.of("minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:cat", "minecraft:wolf", "minecraft:parrot"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("crops", ExceptionGroupCategory.BLOCK,
						List.of("#minecraft:crops"),
						false, true, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("furnaces", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:furnace", "minecraft:smoker", "minecraft:blast_furnace"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("buttons", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:*_button"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("levers", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:lever"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("bells", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:bell"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("lecterns", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:lectern"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("beds", ExceptionGroupCategory.BLOCK,
						List.of("#minecraft:beds"),
						true, false, false, FlagPreset.NOBODY, true),
				// The 6 groups below narrow INTERACT's real footprint further (it was already only a
				// fallback for anything not covered by a group, never a real cascade — see
				// AccessControllerImpl's javadoc): each of these previously had no dedicated group of
				// its own, so it fell all the way through to INTERACT despite being just as
				// "single-purpose" a block as a door or a furnace.
				new ExceptionGroup("crafting_tables", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:crafting_table"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("anvils", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:anvil", "minecraft:chipped_anvil", "minecraft:damaged_anvil"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("enchanting_tables", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:enchanting_table"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("jukeboxes", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:jukebox"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("note_blocks", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:note_block"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("cakes", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:cake"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("barrels", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:barrel"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("shulker_boxes", ExceptionGroupCategory.BLOCK,
						List.of("#minecraft:shulker_boxes"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("hoppers", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:hopper"),
						true, false, false, FlagPreset.NOBODY, true),
				new ExceptionGroup("dispensers_droppers", ExceptionGroupCategory.BLOCK,
						List.of("minecraft:dispenser", "minecraft:dropper"),
						true, false, false, FlagPreset.NOBODY, true)
		);
	}

	private static void writeGroups(Path configFile, List<ExceptionGroup> groups) {
		JsonArray array = new JsonArray();
		groups.forEach(group -> array.add(toJson(group)));

		try {
			Files.createDirectories(configFile.getParent());
			Files.writeString(configFile, GSON.toJson(array));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to write {}", CONFIG_FILE_NAME, e);
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
