package com.skyframework.islandcore.teleport;

import com.skyframework.islandcore.IslandCoreMod;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Per-player, per-dimension last position (Sprint "teletransportes dinámicos"): recorded by
// PlayerLastPositionMixin right before a player leaves a dimension, and consulted by
// TeleportManagerImpl#requestDimensionTeleport so a player's second-or-later visit to a dynamic
// dimension resumes where they left off instead of always landing back at the world's spawn point.
// Same single-shared-file, load-on-SERVER_STARTED, save-on-every-write pattern as
// PlayerLocationSharingConfig (<world>/islandcore/last_positions.dat).
public class PlayerLastPositionStore {

	private static final String FILE_NAME = "last_positions.dat";

	private final Map<UUID, Map<Identifier, BlockPos>> byPlayer = new HashMap<>();

	private Path file;

	public PlayerLastPositionStore() {
		ServerLifecycleEvents.SERVER_STARTED.register(startedServer ->
				load(startedServer.getSavePath(WorldSavePath.ROOT).resolve("islandcore").resolve(FILE_NAME)));
	}

	private void load(Path file) {
		this.file = file;

		if (!Files.exists(file)) {
			return;
		}

		try {
			NbtCompound nbt = NbtIo.readCompressed(file, NbtSizeTracker.ofUnlimitedBytes());
			for (String playerKey : nbt.getKeys()) {
				try {
					UUID playerUuid = UUID.fromString(playerKey);
					NbtCompound perDimension = nbt.getCompound(playerKey);
					Map<Identifier, BlockPos> positions = new HashMap<>();
					for (String dimensionKey : perDimension.getKeys()) {
						try {
							Identifier dimensionId = Identifier.of(dimensionKey);
							NbtCompound posNbt = perDimension.getCompound(dimensionKey);
							positions.put(dimensionId, new BlockPos(posNbt.getInt("x"), posNbt.getInt("y"), posNbt.getInt("z")));
						} catch (RuntimeException e) {
							IslandCoreMod.LOGGER.error("Skipping invalid dimension key in {}: {}", FILE_NAME, dimensionKey, e);
						}
					}
					byPlayer.put(playerUuid, positions);
				} catch (IllegalArgumentException e) {
					IslandCoreMod.LOGGER.error("Skipping invalid UUID key in {}: {}", FILE_NAME, playerKey, e);
				}
			}
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load {}, every player starts with no remembered position", FILE_NAME, e);
		}
	}

	public Optional<BlockPos> getLastPosition(UUID playerUuid, Identifier dimensionId) {
		Map<Identifier, BlockPos> positions = byPlayer.get(playerUuid);
		if (positions == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(positions.get(dimensionId));
	}

	public void recordLastPosition(UUID playerUuid, Identifier dimensionId, BlockPos pos) {
		byPlayer.computeIfAbsent(playerUuid, uuid -> new HashMap<>()).put(dimensionId, pos);
		save();
	}

	private void save() {
		if (file == null) {
			return;
		}

		NbtCompound nbt = new NbtCompound();
		for (Map.Entry<UUID, Map<Identifier, BlockPos>> playerEntry : byPlayer.entrySet()) {
			NbtCompound perDimension = new NbtCompound();
			for (Map.Entry<Identifier, BlockPos> dimensionEntry : playerEntry.getValue().entrySet()) {
				NbtCompound posNbt = new NbtCompound();
				BlockPos pos = dimensionEntry.getValue();
				posNbt.putInt("x", pos.getX());
				posNbt.putInt("y", pos.getY());
				posNbt.putInt("z", pos.getZ());
				perDimension.put(dimensionEntry.getKey().toString(), posNbt);
			}
			nbt.put(playerEntry.getKey().toString(), perDimension);
		}

		try {
			Files.createDirectories(file.getParent());
			NbtIo.writeCompressed(nbt, file);
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to save {}", FILE_NAME, e);
		}
	}
}
