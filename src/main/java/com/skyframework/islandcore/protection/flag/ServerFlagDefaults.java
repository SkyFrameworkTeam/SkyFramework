package com.skyframework.islandcore.protection.flag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.island.model.IslandRole;

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
//
// Two independent maps, one per FlagCategory: ISLAND_GLOBAL flags keep a single ALLOW/DENY value
// applied uniformly (globalOverrides, unchanged since before this class supported presets);
// ROLE_BASED flags instead store a full 4-level preset (roleBasedOverrides) — reusing FlagPreset,
// the exact same nadie/miembros/aliados/todos mechanism IslandRegistryImpl#applyFlagPreset uses for
// a single island — since a flat single value can't express "different per role" the way a preset
// (or an island's own per-role override) can.
public class ServerFlagDefaults {

	private static final String CONFIG_FILE_NAME = "flag_defaults.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Map<String, TriState> globalOverrides = new HashMap<>();
	private final Map<String, FlagPreset> roleBasedOverrides = new HashMap<>();

	public ServerFlagDefaults() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> load());
	}

	private void load() {
		Path configFile = configFile();

		if (!Files.exists(configFile)) {
			writeDefault(configFile);
			return;
		}

		globalOverrides.clear();
		roleBasedOverrides.clear();

		try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

			// Migrated ahead of the general loop below: "construccion" replaces the separate
			// "build"/"break" flags (see FlagRegistry.CONSTRUCCION), sharing build's default table.
			// A file written before this merge may still have one or both of the old keys, in
			// either the current preset-string format or the older plain ALLOW/DENY format (both
			// handled by resolveRoleBasedPreset) — "build"'s value wins if both are present (matches
			// the requested per-island migration rule), "break"'s is used only as a fallback.
			FlagPreset legacyBuild = root.has("build") ? resolveRoleBasedPreset(root.get("build").getAsString()).orElse(null) : null;
			FlagPreset legacyBreak = root.has("break") ? resolveRoleBasedPreset(root.get("break").getAsString()).orElse(null) : null;
			FlagPreset legacyConstruccion = legacyBuild != null ? legacyBuild : legacyBreak;
			if (legacyConstruccion != null) {
				roleBasedOverrides.put("construccion", legacyConstruccion);
			}

			for (String flagId : root.keySet()) {
				if (flagId.equals("build") || flagId.equals("break")) {
					continue;
				}
				loadEntry(flagId, root.get(flagId).getAsString());
			}
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load {}, using defaults", CONFIG_FILE_NAME, e);
		}
	}

	private void loadEntry(String flagId, String raw) {
		Optional<Flag> flag = FlagRegistry.get(flagId);
		boolean roleBased = flag.isPresent() && flag.get().getCategory() == FlagCategory.ROLE_BASED;

		if (roleBased) {
			resolveRoleBasedPreset(raw).ifPresentOrElse(
					preset -> roleBasedOverrides.put(flagId, preset),
					() -> IslandCoreMod.LOGGER.error("Skipping invalid value for flag \"{}\" in {}", flagId, CONFIG_FILE_NAME));
			return;
		}

		// ISLAND_GLOBAL (or an unknown/removed flag id — kept as a raw TriState rather than silently
		// dropped, in case the flag comes back in a future update).
		try {
			TriState value = TriState.valueOf(raw.toUpperCase(Locale.ROOT));
			if (value != TriState.DEFAULT) {
				globalOverrides.put(flagId, value);
			}
		} catch (IllegalArgumentException e) {
			IslandCoreMod.LOGGER.error("Skipping invalid value for flag \"{}\" in {}", flagId, CONFIG_FILE_NAME, e);
		}
	}

	// Accepts either a preset id (current format) or a legacy plain ALLOW/DENY (pre-preset format,
	// applied uniformly to every role — ALLOW is exactly the "todos" preset, DENY is exactly
	// "nadie", see FlagPreset). DEFAULT has nothing to migrate (it was never persisted).
	private static Optional<FlagPreset> resolveRoleBasedPreset(String raw) {
		Optional<FlagPreset> preset = FlagPreset.fromId(raw.toLowerCase(Locale.ROOT));
		if (preset.isPresent()) {
			return preset;
		}
		try {
			TriState legacy = TriState.valueOf(raw.toUpperCase(Locale.ROOT));
			if (legacy == TriState.ALLOW) {
				return Optional.of(FlagPreset.EVERYONE);
			} else if (legacy == TriState.DENY) {
				return Optional.of(FlagPreset.NOBODY);
			}
			return Optional.empty();
		} catch (IllegalArgumentException e) {
			return Optional.empty();
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

	// ISLAND_GLOBAL only.
	public Optional<TriState> getGlobalDefault(String flagId) {
		return Optional.ofNullable(globalOverrides.get(flagId));
	}

	// ISLAND_GLOBAL only. value == TriState.DEFAULT clears the server-wide override for this flag.
	public void setGlobalDefault(String flagId, TriState value) {
		if (value == TriState.DEFAULT) {
			globalOverrides.remove(flagId);
		} else {
			globalOverrides.put(flagId, value);
		}
		persist();
	}

	// ROLE_BASED only: the resolved value for one role under this flag's server-wide preset, or
	// empty if no preset is set for it (falls through to the flag's own hardcoded default).
	public Optional<TriState> getRoleBasedDefault(String flagId, IslandRole role) {
		FlagPreset preset = roleBasedOverrides.get(flagId);
		if (preset == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(preset.toRoleValues().get(role));
	}

	// ROLE_BASED only, for reporting the current server-wide preset (AdminDefaultsStatusS2C).
	public Optional<FlagPreset> getRoleBasedPreset(String flagId) {
		return Optional.ofNullable(roleBasedOverrides.get(flagId));
	}

	// ROLE_BASED only. preset == null clears the server-wide default for this flag.
	public void setRoleBasedDefault(String flagId, FlagPreset preset) {
		if (preset == null) {
			roleBasedOverrides.remove(flagId);
		} else {
			roleBasedOverrides.put(flagId, preset);
		}
		persist();
	}

	private void persist() {
		JsonObject root = new JsonObject();
		for (Map.Entry<String, TriState> entry : globalOverrides.entrySet()) {
			root.addProperty(entry.getKey(), entry.getValue().name());
		}
		for (Map.Entry<String, FlagPreset> entry : roleBasedOverrides.entrySet()) {
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
