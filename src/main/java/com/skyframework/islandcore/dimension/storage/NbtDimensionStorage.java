package com.skyframework.islandcore.dimension.storage;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.dimension.model.DimensionData;
import com.skyframework.islandcore.dimension.model.DimensionGeneratorStyle;
import com.skyframework.islandcore.dimension.model.DimensionState;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// One compressed NBT file per dimension, mirroring NbtIslandStorage's layout.
public class NbtDimensionStorage implements DimensionStorage {

	private static final int SCHEMA_VERSION = 1;
	private static final String FILE_SUFFIX = ".dat";

	private final Path baseDir;

	public NbtDimensionStorage(Path baseDir) {
		this.baseDir = baseDir;
		try {
			Files.createDirectories(baseDir);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create dimension storage directory: " + baseDir, e);
		}
	}

	@Override
	public void save(DimensionData dimension) {
		try {
			NbtIo.writeCompressed(toNbt(dimension), fileFor(dimension.getId()));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to save dimension {}", dimension.getId(), e);
		}
	}

	@Override
	public void delete(Identifier dimensionId) {
		try {
			Files.deleteIfExists(fileFor(dimensionId));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to delete dimension {}", dimensionId, e);
		}
	}

	@Override
	public Optional<DimensionData> load(Identifier dimensionId) {
		return loadFile(fileFor(dimensionId));
	}

	@Override
	public Collection<DimensionData> loadAll() {
		List<DimensionData> dimensions = new ArrayList<>();

		if (!Files.isDirectory(baseDir)) {
			return dimensions;
		}

		try (Stream<Path> files = Files.list(baseDir)) {
			for (Path file : files.filter(p -> p.getFileName().toString().endsWith(FILE_SUFFIX)).toList()) {
				loadFile(file).ifPresent(dimensions::add);
			}
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to list dimension storage directory: {}", baseDir, e);
		}

		return dimensions;
	}

	private Optional<DimensionData> loadFile(Path file) {
		if (!Files.exists(file)) {
			return Optional.empty();
		}

		try {
			NbtCompound nbt = NbtIo.readCompressed(file, NbtSizeTracker.ofUnlimitedBytes());
			return Optional.of(fromNbt(nbt));
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load dimension file {}, skipping", file, e);
			return Optional.empty();
		}
	}

	// The filename is only a storage key, never a source of truth: the Identifier is always read
	// back from the NBT content itself (see fromNbt/loadFile), so this doesn't need to be
	// reversible. Uses the full "namespace_path" (not just the path) so e.g. "islandcore:farming"
	// can never collide with some other namespace's "othermod:farming" in this shared directory.
	private Path fileFor(Identifier id) {
		String sanitized = (id.getNamespace() + "_" + id.getPath()).replace('/', '_').replace(':', '_');
		return baseDir.resolve(sanitized + FILE_SUFFIX);
	}

	private static NbtCompound toNbt(DimensionData dimension) {
		NbtCompound nbt = new NbtCompound();

		nbt.putInt("schemaVersion", SCHEMA_VERSION);
		nbt.putString("id", dimension.getId().toString());
		nbt.putString("displayName", dimension.getDisplayName());
		nbt.putString("generatorStyle", dimension.getGeneratorStyle().name());
		nbt.putLong("seed", dimension.getSeed());
		nbt.putString("state", dimension.getState().name());
		nbt.putLong("createdAt", dimension.getCreatedAt().toEpochMilli());
		nbt.putLong("updatedAt", dimension.getUpdatedAt().toEpochMilli());

		return nbt;
	}

	private static DimensionData fromNbt(NbtCompound nbt) {
		Identifier id = Identifier.of(nbt.getString("id"));
		String displayName = nbt.getString("displayName");
		DimensionGeneratorStyle generatorStyle = DimensionGeneratorStyle.valueOf(nbt.getString("generatorStyle"));
		long seed = nbt.getLong("seed");
		DimensionState state = DimensionState.valueOf(nbt.getString("state"));
		Instant createdAt = Instant.ofEpochMilli(nbt.getLong("createdAt"));
		Instant updatedAt = Instant.ofEpochMilli(nbt.getLong("updatedAt"));

		return new DimensionData(id, displayName, generatorStyle, seed, state, createdAt, updatedAt);
	}
}
