package com.skyframework.islandcore.storage.nbt;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.IslandPermission;
import com.skyframework.islandcore.api.island.IslandState;
import com.skyframework.islandcore.island.model.IslandBounds;
import com.skyframework.islandcore.island.model.IslandData;
import com.skyframework.islandcore.island.model.IslandMember;
import com.skyframework.islandcore.island.model.IslandRole;
import com.skyframework.islandcore.island.model.IslandSetting;
import com.skyframework.islandcore.island.model.IslandType;
import com.skyframework.islandcore.protection.flag.FlagPreset;
import com.skyframework.islandcore.protection.flag.TriState;
import com.skyframework.islandcore.storage.IslandStorage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class NbtIslandStorage implements IslandStorage {

	private static final int SCHEMA_VERSION = 1;
	private static final String FILE_SUFFIX = ".dat";

	private final Path baseDir;

	public NbtIslandStorage(Path baseDir) {
		this.baseDir = baseDir;
		try {
			Files.createDirectories(baseDir);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create island storage directory: " + baseDir, e);
		}
	}

	@Override
	public void save(IslandData island) {
		try {
			NbtIo.writeCompressed(toNbt(island), fileFor(island.getIslandId()));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to save island {}", island.getIslandId(), e);
		}
	}

	@Override
	public void delete(UUID islandId) {
		try {
			Files.deleteIfExists(fileFor(islandId));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to delete island {}", islandId, e);
		}
	}

	@Override
	public Optional<IslandData> load(UUID islandId) {
		Path file = fileFor(islandId);
		if (!Files.exists(file)) {
			return Optional.empty();
		}

		try {
			NbtCompound nbt = NbtIo.readCompressed(file, NbtSizeTracker.ofUnlimitedBytes());
			return Optional.of(fromNbt(nbt));
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load island {}, skipping", islandId, e);
			return Optional.empty();
		}
	}

	@Override
	public Collection<IslandData> loadAll() {
		List<IslandData> islands = new ArrayList<>();

		if (!Files.isDirectory(baseDir)) {
			return islands;
		}

		try (Stream<Path> files = Files.list(baseDir)) {
			for (Path file : files.filter(p -> p.getFileName().toString().endsWith(FILE_SUFFIX)).toList()) {
				String fileName = file.getFileName().toString();
				String idPart = fileName.substring(0, fileName.length() - FILE_SUFFIX.length());
				try {
					load(UUID.fromString(idPart)).ifPresent(islands::add);
				} catch (IllegalArgumentException e) {
					IslandCoreMod.LOGGER.error("Skipping island file with invalid name: {}", fileName, e);
				}
			}
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to list island storage directory: {}", baseDir, e);
		}

		return islands;
	}

	private Path fileFor(UUID islandId) {
		return baseDir.resolve(islandId + FILE_SUFFIX);
	}

	private static NbtCompound toNbt(IslandData island) {
		NbtCompound nbt = new NbtCompound();

		nbt.putInt("schemaVersion", SCHEMA_VERSION);
		nbt.putUuid("islandId", island.getIslandId());
		nbt.putUuid("ownerUuid", island.getOwnerUuid());
		nbt.putString("dimension", island.getDimension().getValue().toString());
		nbt.putInt("gridX", island.getGridX());
		nbt.putInt("gridZ", island.getGridZ());
		nbt.put("center", posToNbt(island.getCenter()));
		nbt.put("bounds", boundsToNbt(island.getBounds()));
		nbt.put("plotBounds", boundsToNbt(island.getPlotBounds()));
		nbt.putInt("islandSize", island.getIslandSize());
		nbt.putInt("plotSize", island.getPlotSize());
		nbt.putString("islandType", island.getIslandType().getId());
		nbt.put("homeLocation", posToNbt(island.getHomeLocation()));
		nbt.putString("currentBiomeId", island.getCurrentBiomeId());
		if (island.getLastBiomeChangeAt() != null) {
			nbt.putLong("lastBiomeChangeAt", island.getLastBiomeChangeAt().toEpochMilli());
		}
		nbt.putString("state", island.getState().name());
		nbt.put("members", membersToNbt(island.getMembers()));
		nbt.put("settings", settingsToNbt(island));
		nbt.put("globalFlagOverrides", globalFlagOverridesToNbt(island));
		nbt.put("roleFlagOverrides", roleFlagOverridesToNbt(island));
		nbt.put("exceptionGroupOverrides", exceptionGroupOverridesToNbt(island));
		nbt.putLong("createdAt", island.getCreatedAt().toEpochMilli());
		nbt.putLong("updatedAt", island.getUpdatedAt().toEpochMilli());

		return nbt;
	}

	private static IslandData fromNbt(NbtCompound nbt) {
		UUID islandId = nbt.getUuid("islandId");
		UUID ownerUuid = nbt.getUuid("ownerUuid");
		RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(nbt.getString("dimension")));
		int gridX = nbt.getInt("gridX");
		int gridZ = nbt.getInt("gridZ");
		BlockPos center = posFromNbt(nbt.getCompound("center"));
		IslandBounds bounds = boundsFromNbt(nbt.getCompound("bounds"));
		IslandBounds plotBounds = boundsFromNbt(nbt.getCompound("plotBounds"));
		int islandSize = nbt.getInt("islandSize");
		int plotSize = nbt.getInt("plotSize");
		IslandType islandType = IslandType.fromId(nbt.getString("islandType"));
		BlockPos homeLocation = posFromNbt(nbt.getCompound("homeLocation"));
		// Absent on islands saved before this field existed: falls back to the same default a
		// never-changed island gets at creation (see IslandData.DEFAULT_BIOME_ID).
		String currentBiomeId = nbt.contains("currentBiomeId", NbtElement.STRING_TYPE)
				? nbt.getString("currentBiomeId")
				: IslandData.DEFAULT_BIOME_ID;
		// Absent on islands saved before this field existed: null means "never changed".
		Instant lastBiomeChangeAt = nbt.contains("lastBiomeChangeAt", NbtElement.LONG_TYPE)
				? Instant.ofEpochMilli(nbt.getLong("lastBiomeChangeAt"))
				: null;
		IslandState state = IslandState.valueOf(nbt.getString("state"));
		List<IslandMember> members = membersFromNbt(nbt.getList("members", NbtElement.COMPOUND_TYPE));
		// "settings" didn't exist before this was added: islands saved earlier simply have no
		// such key, and settingsFromNbt returns an empty map for that (falls back to per-setting
		// defaults at read time via IslandData.getSetting()).
		Map<IslandSetting, Boolean> settings = settingsFromNbt(nbt.getCompound("settings"));
		Instant createdAt = Instant.ofEpochMilli(nbt.getLong("createdAt"));
		Instant updatedAt = Instant.ofEpochMilli(nbt.getLong("updatedAt"));

		// Absent on islands saved before the flag system existed: empty compounds parse to empty maps.
		Map<String, TriState> globalFlagOverrides = globalFlagOverridesFromNbt(nbt.getCompound("globalFlagOverrides"));
		Map<String, Map<IslandRole, TriState>> roleFlagOverrides = roleFlagOverridesFromNbt(nbt.getCompound("roleFlagOverrides"));
		Map<String, Map<IslandRole, TriState>> roleExceptionGroupOverrides = roleExceptionGroupOverridesFromNbt(nbt.getCompound("exceptionGroupOverrides"));

		// Backward-compat: before the flag system existed, /island settings firespread/pvp/mobdamage
		// wrote directly into the "settings" compound above (keyed by the old IslandSetting enum).
		// If the new global flag override is absent but the legacy setting was EXPLICITLY set (not
		// just defaulted), seed the new map from it so past changes keep applying after this migration.
		NbtCompound legacySettingsNbt = nbt.getCompound("settings");
		migrateLegacyGlobalFlag(globalFlagOverrides, legacySettingsNbt, IslandSetting.FIRE_SPREAD, "fire_spread");
		migrateLegacyGlobalFlag(globalFlagOverrides, legacySettingsNbt, IslandSetting.PVP_DAMAGE, "pvp_damage");
		migrateLegacyGlobalFlag(globalFlagOverrides, legacySettingsNbt, IslandSetting.MOB_DAMAGE, "mob_damage");

		return new IslandData(
				islandId, ownerUuid, dimension, gridX, gridZ, center, bounds, plotBounds,
				islandSize, plotSize, islandType, homeLocation, state, createdAt, updatedAt, members, settings,
				lastBiomeChangeAt, currentBiomeId, globalFlagOverrides, roleFlagOverrides, roleExceptionGroupOverrides
		);
	}

	private static void migrateLegacyGlobalFlag(
			Map<String, TriState> globalFlagOverrides, NbtCompound legacySettingsNbt, IslandSetting legacySetting, String flagId) {
		if (globalFlagOverrides.containsKey(flagId)) {
			// Already has a new-format value: don't clobber it with a possibly-stale legacy one.
			return;
		}
		if (!legacySettingsNbt.contains(legacySetting.name(), NbtElement.BYTE_TYPE)) {
			// Never explicitly set under the old system either: nothing to migrate.
			return;
		}
		globalFlagOverrides.put(flagId, TriState.fromBoolean(legacySettingsNbt.getBoolean(legacySetting.name())));
	}

	private static NbtCompound posToNbt(BlockPos pos) {
		NbtCompound nbt = new NbtCompound();
		nbt.putInt("x", pos.getX());
		nbt.putInt("y", pos.getY());
		nbt.putInt("z", pos.getZ());
		return nbt;
	}

	private static BlockPos posFromNbt(NbtCompound nbt) {
		return new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
	}

	private static NbtCompound boundsToNbt(IslandBounds bounds) {
		NbtCompound nbt = new NbtCompound();
		nbt.put("min", posToNbt(bounds.min()));
		nbt.put("max", posToNbt(bounds.max()));
		return nbt;
	}

	private static IslandBounds boundsFromNbt(NbtCompound nbt) {
		return new IslandBounds(posFromNbt(nbt.getCompound("min")), posFromNbt(nbt.getCompound("max")));
	}

	private static NbtList membersToNbt(Collection<IslandMember> members) {
		NbtList list = new NbtList();

		for (IslandMember member : members) {
			NbtCompound memberNbt = new NbtCompound();
			memberNbt.putUuid("playerUuid", member.playerUuid());
			memberNbt.putString("role", member.role().name());
			memberNbt.putLong("addedAt", member.addedAt().toEpochMilli());

			NbtList overrides = new NbtList();
			for (IslandPermission permission : member.overrides()) {
				overrides.add(NbtString.of(permission.name()));
			}
			memberNbt.put("overrides", overrides);

			list.add(memberNbt);
		}

		return list;
	}

	private static List<IslandMember> membersFromNbt(NbtList list) {
		List<IslandMember> members = new ArrayList<>();

		for (int i = 0; i < list.size(); i++) {
			NbtCompound memberNbt = list.getCompound(i);

			UUID playerUuid = memberNbt.getUuid("playerUuid");
			IslandRole role = IslandRole.valueOf(memberNbt.getString("role"));
			Instant addedAt = Instant.ofEpochMilli(memberNbt.getLong("addedAt"));

			EnumSet<IslandPermission> overrides = EnumSet.noneOf(IslandPermission.class);
			NbtList overridesNbt = memberNbt.getList("overrides", NbtElement.STRING_TYPE);
			for (int j = 0; j < overridesNbt.size(); j++) {
				String raw = overridesNbt.getString(j);
				try {
					overrides.add(IslandPermission.valueOf(raw));
				} catch (IllegalArgumentException e) {
					// Tolerant of the pre-merge schema: "BUILD"/"BREAK" collapsed into CONSTRUCCION
					// (see IslandPermission) — translate rather than drop. In practice no code path
					// has ever populated this per-member override set (every IslandMember is created
					// with EnumSet.noneOf), so this is precautionary, not a known-real case.
					if ("BUILD".equals(raw) || "BREAK".equals(raw)) {
						overrides.add(IslandPermission.CONSTRUCCION);
					} else {
						IslandCoreMod.LOGGER.error("Skipping invalid member permission override \"{}\"", raw);
					}
				}
			}

			members.add(new IslandMember(playerUuid, role, addedAt, overrides));
		}

		return members;
	}

	private static NbtCompound settingsToNbt(IslandData island) {
		NbtCompound nbt = new NbtCompound();
		for (IslandSetting setting : IslandSetting.values()) {
			nbt.putBoolean(setting.name(), island.getSetting(setting));
		}
		return nbt;
	}

	private static Map<IslandSetting, Boolean> settingsFromNbt(NbtCompound nbt) {
		Map<IslandSetting, Boolean> settings = new EnumMap<>(IslandSetting.class);
		for (IslandSetting setting : IslandSetting.values()) {
			if (nbt.contains(setting.name(), NbtElement.BYTE_TYPE)) {
				settings.put(setting, nbt.getBoolean(setting.name()));
			}
		}
		return settings;
	}

	private static NbtCompound globalFlagOverridesToNbt(IslandData island) {
		NbtCompound nbt = new NbtCompound();
		for (Map.Entry<String, TriState> entry : island.getGlobalFlagOverrides().entrySet()) {
			nbt.putString(entry.getKey(), entry.getValue().name());
		}
		return nbt;
	}

	private static Map<String, TriState> globalFlagOverridesFromNbt(NbtCompound nbt) {
		Map<String, TriState> overrides = new HashMap<>();
		for (String flagId : nbt.getKeys()) {
			try {
				overrides.put(flagId, TriState.valueOf(nbt.getString(flagId)));
			} catch (IllegalArgumentException e) {
				IslandCoreMod.LOGGER.error("Skipping invalid global flag override \"{}\"", flagId, e);
			}
		}
		return overrides;
	}

	private static NbtCompound roleFlagOverridesToNbt(IslandData island) {
		NbtCompound nbt = new NbtCompound();
		for (Map.Entry<String, Map<IslandRole, TriState>> entry : island.getRoleFlagOverrides().entrySet()) {
			NbtCompound perRoleNbt = new NbtCompound();
			for (Map.Entry<IslandRole, TriState> roleEntry : entry.getValue().entrySet()) {
				perRoleNbt.putString(roleEntry.getKey().name(), roleEntry.getValue().name());
			}
			nbt.put(entry.getKey(), perRoleNbt);
		}
		return nbt;
	}

	private static Map<String, Map<IslandRole, TriState>> roleFlagOverridesFromNbt(NbtCompound nbt) {
		Map<String, Map<IslandRole, TriState>> overrides = new HashMap<>();
		for (String flagId : nbt.getKeys()) {
			NbtCompound perRoleNbt = nbt.getCompound(flagId);
			Map<IslandRole, TriState> perRole = new EnumMap<>(IslandRole.class);
			for (String roleName : perRoleNbt.getKeys()) {
				try {
					perRole.put(IslandRole.valueOf(roleName), TriState.valueOf(perRoleNbt.getString(roleName)));
				} catch (IllegalArgumentException e) {
					IslandCoreMod.LOGGER.error("Skipping invalid role flag override \"{}\"/\"{}\"", flagId, roleName, e);
				}
			}
			if (!perRole.isEmpty()) {
				overrides.put(flagId, perRole);
			}
		}
		return migrateConstruccionOverride(overrides);
	}

	// Tolerant of the pre-merge schema: "construccion" replaces the separate "build"/"break" flags
	// (see FlagRegistry.CONSTRUCCION). "build"'s override wins if the island had both; "break"'s is
	// used only as a fallback when the island never had its own "build" override — matches the
	// requested per-island migration rule exactly. Both old keys are removed either way so they
	// don't linger as dead per-flag overrides for a flag id that no longer exists.
	private static Map<String, Map<IslandRole, TriState>> migrateConstruccionOverride(Map<String, Map<IslandRole, TriState>> overrides) {
		Map<IslandRole, TriState> buildOverride = overrides.remove("build");
		Map<IslandRole, TriState> breakOverride = overrides.remove("break");
		Map<IslandRole, TriState> migrated = buildOverride != null ? buildOverride : breakOverride;
		if (migrated != null && !overrides.containsKey("construccion")) {
			overrides.put("construccion", migrated);
		}
		return overrides;
	}

	private static NbtCompound exceptionGroupOverridesToNbt(IslandData island) {
		NbtCompound nbt = new NbtCompound();
		for (Map.Entry<String, Map<IslandRole, TriState>> entry : island.getRoleExceptionGroupOverrides().entrySet()) {
			NbtCompound perRoleNbt = new NbtCompound();
			for (Map.Entry<IslandRole, TriState> roleEntry : entry.getValue().entrySet()) {
				perRoleNbt.putString(roleEntry.getKey().name(), roleEntry.getValue().name());
			}
			nbt.put(entry.getKey(), perRoleNbt);
		}
		return nbt;
	}

	// Tolerant of the pre-role-based schema: a group stored as a plain boolean (NbtElement.BYTE_TYPE,
	// from before exception groups resolved per role) is translated into a uniform per-role
	// override — true meant "on for everyone" (every role ALLOW, matching FlagPreset.EVERYONE),
	// false meant "off for everyone" (FlagPreset.NOBODY) — the same lossless translation
	// ExceptionGroupRegistry applies to the config file's old defaultEnabled field. A group already
	// in the new nested-compound shape (NbtElement.COMPOUND_TYPE) is read exactly like
	// roleFlagOverridesFromNbt above.
	private static Map<String, Map<IslandRole, TriState>> roleExceptionGroupOverridesFromNbt(NbtCompound nbt) {
		Map<String, Map<IslandRole, TriState>> overrides = new HashMap<>();
		for (String groupId : nbt.getKeys()) {
			if (nbt.contains(groupId, NbtElement.COMPOUND_TYPE)) {
				NbtCompound perRoleNbt = nbt.getCompound(groupId);
				Map<IslandRole, TriState> perRole = new EnumMap<>(IslandRole.class);
				for (String roleName : perRoleNbt.getKeys()) {
					try {
						perRole.put(IslandRole.valueOf(roleName), TriState.valueOf(perRoleNbt.getString(roleName)));
					} catch (IllegalArgumentException e) {
						IslandCoreMod.LOGGER.error("Skipping invalid role exception group override \"{}\"/\"{}\"", groupId, roleName, e);
					}
				}
				if (!perRole.isEmpty()) {
					overrides.put(groupId, perRole);
				}
			} else if (nbt.contains(groupId, NbtElement.BYTE_TYPE)) {
				boolean legacyEnabled = nbt.getBoolean(groupId);
				overrides.put(groupId, (legacyEnabled ? FlagPreset.EVERYONE : FlagPreset.NOBODY).toRoleValues());
			}
		}
		return overrides;
	}
}
