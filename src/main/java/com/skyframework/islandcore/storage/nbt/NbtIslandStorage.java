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
		if (island.getLastBiomeChangeAt() != null) {
			nbt.putLong("lastBiomeChangeAt", island.getLastBiomeChangeAt().toEpochMilli());
		}
		nbt.putString("state", island.getState().name());
		nbt.put("members", membersToNbt(island.getMembers()));
		nbt.put("settings", settingsToNbt(island));
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

		return new IslandData(
				islandId, ownerUuid, dimension, gridX, gridZ, center, bounds, plotBounds,
				islandSize, plotSize, islandType, homeLocation, state, createdAt, updatedAt, members, settings,
				lastBiomeChangeAt
		);
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
				overrides.add(IslandPermission.valueOf(overridesNbt.getString(j)));
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
}
