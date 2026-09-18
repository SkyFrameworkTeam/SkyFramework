package com.skyframework.islandcore.player;

import com.skyframework.islandcore.IslandCoreMod;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

// Per-player location-sharing preferences (all four default false), now split into an independent
// party pair and allies pair instead of one shared toggle for both relationships: whether THIS
// player's own position is exposed to their party (sendPositionToParty) / their island's allies
// (sendPositionToAllies), and whether THIS player receives their party's positions
// (receivePositionsFromParty) / their island's allies' positions (receivePositionsFromAllies) for
// the HUD indicator — see AllyLocationBroadcaster, the only reader of these. Same single-shared-file,
// load-on-SERVER_STARTED pattern as FirstJoinTracker (<world>/islandcore/location_sharing.dat).
public class PlayerLocationSharingConfig {

	private static final String FILE_NAME = "location_sharing.dat";

	private record Entry(
			boolean sendPositionToParty,
			boolean receivePositionsFromParty,
			boolean sendPositionToAllies,
			boolean receivePositionsFromAllies
	) {
		static final Entry DEFAULT = new Entry(false, false, false, false);
	}

	private final Map<UUID, Entry> byPlayer = new HashMap<>();

	private Path file;

	public PlayerLocationSharingConfig() {
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
			for (String key : nbt.getKeys()) {
				try {
					UUID playerUuid = UUID.fromString(key);
					NbtCompound entryNbt = nbt.getCompound(key);
					byPlayer.put(playerUuid, new Entry(
							entryNbt.getBoolean("sendPositionToParty"),
							entryNbt.getBoolean("receivePositionsFromParty"),
							entryNbt.getBoolean("sendPositionToAllies"),
							entryNbt.getBoolean("receivePositionsFromAllies")));
				} catch (IllegalArgumentException e) {
					IslandCoreMod.LOGGER.error("Skipping invalid UUID key in {}: {}", FILE_NAME, key, e);
				}
			}
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load {}, every player defaults to sharing disabled", FILE_NAME, e);
		}
	}

	public boolean isSendPositionToPartyEnabled(UUID playerUuid) {
		return byPlayer.getOrDefault(playerUuid, Entry.DEFAULT).sendPositionToParty();
	}

	public boolean isReceivePositionsFromPartyEnabled(UUID playerUuid) {
		return byPlayer.getOrDefault(playerUuid, Entry.DEFAULT).receivePositionsFromParty();
	}

	public boolean isSendPositionToAlliesEnabled(UUID playerUuid) {
		return byPlayer.getOrDefault(playerUuid, Entry.DEFAULT).sendPositionToAllies();
	}

	public boolean isReceivePositionsFromAlliesEnabled(UUID playerUuid) {
		return byPlayer.getOrDefault(playerUuid, Entry.DEFAULT).receivePositionsFromAllies();
	}

	public void setSendPositionToParty(UUID playerUuid, boolean value) {
		update(playerUuid, current -> new Entry(value, current.receivePositionsFromParty(), current.sendPositionToAllies(), current.receivePositionsFromAllies()));
	}

	public void setReceivePositionsFromParty(UUID playerUuid, boolean value) {
		update(playerUuid, current -> new Entry(current.sendPositionToParty(), value, current.sendPositionToAllies(), current.receivePositionsFromAllies()));
	}

	public void setSendPositionToAllies(UUID playerUuid, boolean value) {
		update(playerUuid, current -> new Entry(current.sendPositionToParty(), current.receivePositionsFromParty(), value, current.receivePositionsFromAllies()));
	}

	public void setReceivePositionsFromAllies(UUID playerUuid, boolean value) {
		update(playerUuid, current -> new Entry(current.sendPositionToParty(), current.receivePositionsFromParty(), current.sendPositionToAllies(), value));
	}

	private void update(UUID playerUuid, UnaryOperator<Entry> updater) {
		Entry current = byPlayer.getOrDefault(playerUuid, Entry.DEFAULT);
		byPlayer.put(playerUuid, updater.apply(current));
		save();
	}

	private void save() {
		if (file == null) {
			return;
		}

		NbtCompound nbt = new NbtCompound();
		for (Map.Entry<UUID, Entry> mapEntry : byPlayer.entrySet()) {
			NbtCompound entryNbt = new NbtCompound();
			entryNbt.putBoolean("sendPositionToParty", mapEntry.getValue().sendPositionToParty());
			entryNbt.putBoolean("receivePositionsFromParty", mapEntry.getValue().receivePositionsFromParty());
			entryNbt.putBoolean("sendPositionToAllies", mapEntry.getValue().sendPositionToAllies());
			entryNbt.putBoolean("receivePositionsFromAllies", mapEntry.getValue().receivePositionsFromAllies());
			nbt.put(mapEntry.getKey().toString(), entryNbt);
		}

		try {
			Files.createDirectories(file.getParent());
			NbtIo.writeCompressed(nbt, file);
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to save {}", FILE_NAME, e);
		}
	}
}
