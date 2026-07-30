package com.skyframework.islandcore.player;

import com.skyframework.islandcore.IslandCoreMod;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// Tracks every player UUID ever seen on this server, in a single small NBT file independent of
// island/ and dimension/ (<world>/islandcore/known_players.dat). Deliberately self-contained: no
// per-player files, no schema versioning needed for something this simple.
public class FirstJoinTracker {

	private static final String FILE_NAME = "known_players.dat";
	private static final String LIST_KEY = "knownPlayers";

	private final Set<UUID> knownPlayers = new HashSet<>();

	private Path file;

	public FirstJoinTracker() {
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
			NbtList list = nbt.getList(LIST_KEY, NbtElement.STRING_TYPE);
			for (int i = 0; i < list.size(); i++) {
				String raw = list.getString(i);
				try {
					knownPlayers.add(UUID.fromString(raw));
				} catch (IllegalArgumentException e) {
					IslandCoreMod.LOGGER.error("Skipping invalid UUID in {}: {}", FILE_NAME, raw, e);
				}
			}
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load {}, treating every player as a first join", FILE_NAME, e);
		}
	}

	// Atomically checks-and-registers: returns true only the very first time this UUID is ever
	// seen, persisting that fact immediately so a crash right after doesn't lose it.
	public boolean isFirstJoin(UUID playerUuid) {
		if (knownPlayers.contains(playerUuid)) {
			return false;
		}

		knownPlayers.add(playerUuid);
		save();
		return true;
	}

	private void save() {
		if (file == null) {
			return;
		}

		NbtList list = new NbtList();
		for (UUID uuid : knownPlayers) {
			list.add(NbtString.of(uuid.toString()));
		}

		NbtCompound nbt = new NbtCompound();
		nbt.put(LIST_KEY, list);

		try {
			Files.createDirectories(file.getParent());
			NbtIo.writeCompressed(nbt, file);
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to save {}", FILE_NAME, e);
		}
	}
}
