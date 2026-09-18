package com.skyframework.islandcore.party.storage;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.party.model.PartyData;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.nbt.NbtString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

// One compressed NBT file per party, mirroring NbtDimensionStorage's layout: <world>/islandcore/parties/<id>.dat.
public class NbtPartyStorage implements PartyStorage {

	private static final int SCHEMA_VERSION = 1;
	private static final String FILE_SUFFIX = ".dat";

	private final Path baseDir;

	public NbtPartyStorage(Path baseDir) {
		this.baseDir = baseDir;
		try {
			Files.createDirectories(baseDir);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create party storage directory: " + baseDir, e);
		}
	}

	@Override
	public void save(PartyData party) {
		try {
			NbtIo.writeCompressed(toNbt(party), fileFor(party.getPartyId()));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to save party {}", party.getPartyId(), e);
		}
	}

	@Override
	public void delete(UUID partyId) {
		try {
			Files.deleteIfExists(fileFor(partyId));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to delete party {}", partyId, e);
		}
	}

	@Override
	public Optional<PartyData> load(UUID partyId) {
		return loadFile(fileFor(partyId));
	}

	@Override
	public Collection<PartyData> loadAll() {
		List<PartyData> parties = new ArrayList<>();

		if (!Files.isDirectory(baseDir)) {
			return parties;
		}

		try (Stream<Path> files = Files.list(baseDir)) {
			for (Path file : files.filter(p -> p.getFileName().toString().endsWith(FILE_SUFFIX)).toList()) {
				loadFile(file).ifPresent(parties::add);
			}
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to list party storage directory: {}", baseDir, e);
		}

		return parties;
	}

	private Optional<PartyData> loadFile(Path file) {
		if (!Files.exists(file)) {
			return Optional.empty();
		}

		try {
			NbtCompound nbt = NbtIo.readCompressed(file, NbtSizeTracker.ofUnlimitedBytes());
			return Optional.of(fromNbt(nbt));
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load party file {}, skipping", file, e);
			return Optional.empty();
		}
	}

	// The filename is only a storage key, never a source of truth: the UUID is always read back
	// from the NBT content itself (see fromNbt/loadFile).
	private Path fileFor(UUID partyId) {
		return baseDir.resolve(partyId + FILE_SUFFIX);
	}

	private static NbtCompound toNbt(PartyData party) {
		NbtCompound nbt = new NbtCompound();

		nbt.putInt("schemaVersion", SCHEMA_VERSION);
		nbt.putUuid("partyId", party.getPartyId());
		nbt.putString("name", party.getName());
		nbt.putUuid("leaderUuid", party.getLeaderUuid());
		nbt.put("members", uuidListToNbt(party.getMembers()));
		nbt.putLong("createdAt", party.getCreatedAt().toEpochMilli());
		nbt.putLong("updatedAt", party.getUpdatedAt().toEpochMilli());

		return nbt;
	}

	private static PartyData fromNbt(NbtCompound nbt) {
		UUID partyId = nbt.getUuid("partyId");
		String name = nbt.getString("name");
		UUID leaderUuid = nbt.getUuid("leaderUuid");
		Set<UUID> members = uuidListFromNbt(nbt.getList("members", NbtElement.STRING_TYPE));
		Instant createdAt = Instant.ofEpochMilli(nbt.getLong("createdAt"));
		Instant updatedAt = Instant.ofEpochMilli(nbt.getLong("updatedAt"));

		return new PartyData(partyId, name, leaderUuid, members, createdAt, updatedAt);
	}

	private static NbtList uuidListToNbt(Collection<UUID> uuids) {
		NbtList list = new NbtList();
		for (UUID uuid : uuids) {
			list.add(NbtString.of(uuid.toString()));
		}
		return list;
	}

	private static Set<UUID> uuidListFromNbt(NbtList list) {
		Set<UUID> uuids = new LinkedHashSet<>();
		for (int i = 0; i < list.size(); i++) {
			uuids.add(UUID.fromString(list.getString(i)));
		}
		return uuids;
	}
}
