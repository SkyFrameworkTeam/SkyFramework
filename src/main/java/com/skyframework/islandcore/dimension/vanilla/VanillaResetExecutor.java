package com.skyframework.islandcore.dimension.vanilla;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

// Called from the very start of IslandCoreMod.onInitialize(), before anything else touches a
// world or server. Sprint 18 research (Fabric Loader 0.19.3 EntrypointPatch source) confirmed the
// "main" entrypoint hook is injected into net.minecraft.server.Main.main() BEFORE server.properties
// is even loaded on 20w22a+ servers (which 1.21.1 is) — i.e. well before level.dat is read or any
// ServerWorld is created this boot. That makes this the only safe moment to delete dimension save
// folders and/or rewrite level.dat's seed for a reset requested during the PREVIOUS run.
public class VanillaResetExecutor {

	private VanillaResetExecutor() {
	}

	public static void executeIfPending() {
		List<PendingVanillaReset> queue = VanillaResetService.readQueue();
		if (queue.isEmpty()) {
			return;
		}

		Path runDir = FabricLoader.getInstance().getGameDir();
		Path worldDir;
		try {
			worldDir = runDir.resolve(readLevelName(runDir));
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to read server.properties, leaving the pending vanilla reset queue in place to retry on the next boot", e);
			return;
		}

		// Only one seed can ever be applied to level.dat this boot: of every queued entry that
		// specifies one, the most recently confirmed wins. Applied FIRST, before any per-dimension
		// folder is touched, and computed from whatever is still in the queue right now (on a retry
		// after a previous partial failure, entries already completed are gone from it, but their
		// seed effect — if they provided the winning one — was already durably written to level.dat
		// in that earlier pass, so recomputing from the remainder here is still correct).
		Long selectedSeed = selectSeed(queue);
		if (selectedSeed != null) {
			try {
				rewriteSeed(worldDir.resolve("level.dat"), selectedSeed);
				IslandCoreMod.LOGGER.info("Vanilla reset: level.dat seed rewritten to {}", selectedSeed);
			} catch (IOException | RuntimeException e) {
				IslandCoreMod.LOGGER.error("Failed to rewrite level.dat's seed, leaving the pending vanilla reset queue in place to retry on the next boot", e);
				return;
			}
		}

		List<PendingVanillaReset> remaining = new ArrayList<>(queue);
		for (Iterator<PendingVanillaReset> it = remaining.iterator(); it.hasNext(); ) {
			PendingVanillaReset pending = it.next();

			try {
				applyDimensionReset(worldDir, pending);
			} catch (IOException | RuntimeException e) {
				// Leave this entry (and everything after it) in the persisted queue: the next boot
				// retries starting from here instead of silently losing the request.
				IslandCoreMod.LOGGER.error("Vanilla reset for dimension \"{}\" failed midway, will retry on the next boot",
						pending.getDimensionKey(), e);
				return;
			}

			// Checkpoint immediately after each success: if a LATER entry fails, this one must not
			// be reprocessed on the next boot.
			it.remove();
			VanillaResetService.writeQueue(remaining);
			IslandCoreMod.LOGGER.info("Vanilla reset applied for dimension \"{}\"", pending.getDimensionKey());
		}

		try {
			Files.deleteIfExists(VanillaResetService.pendingFilePath());
		} catch (IOException e) {
			// Harmless either way: an empty-array leftover file is a no-op on the next boot.
			IslandCoreMod.LOGGER.error("Failed to delete the now-empty pending vanilla reset file", e);
		}
	}

	// Picks the seed from the most recently confirmed entry among those that specify one (null if
	// none do, meaning level.dat's seed is left untouched).
	private static Long selectSeed(List<PendingVanillaReset> queue) {
		PendingVanillaReset latestWithSeed = null;
		for (PendingVanillaReset pending : queue) {
			if (pending.getSeed() == null) {
				continue;
			}
			if (latestWithSeed == null || pending.getTimestamp().isAfter(latestWithSeed.getTimestamp())) {
				latestWithSeed = pending;
			}
		}
		return latestWithSeed != null ? latestWithSeed.getSeed() : null;
	}

	// Per-dimension work only: player relocation + folder deletion. The seed (if any) is handled
	// once, globally, by executeIfPending() itself before this runs — see selectSeed() above.
	private static void applyDimensionReset(Path worldDir, PendingVanillaReset pending) throws IOException {
		// Relocate offline players out of the dimension being wiped BEFORE touching any of its
		// files: their last-saved position/dimension would otherwise point at terrain that's about
		// to be deleted and regenerated differently. Connected players are already handled
		// separately, immediately on confirmation, by VanillaResetService.
		RelocationTarget relocationTarget = resolveRelocationTarget(worldDir, pending.getDimensionKey());
		relocateOfflinePlayers(worldDir, pending.getDimensionKey(), relocationTarget);

		for (Path folder : foldersFor(pending.getDimensionKey(), worldDir)) {
			deleteRecursively(folder);
		}
	}

	// dimensionId ("islandcore:islands", "minecraft:overworld", ...) matches whatever a player's
	// own "Dimension" NBT tag holds — see relocateOfflinePlayers.
	private record RelocationTarget(String dimensionId, BlockPos pos) {
	}

	private static RelocationTarget resolveRelocationTarget(Path worldDir, String dimensionKeyBeingReset) throws IOException {
		Optional<RelocationTarget> spawnIslandHome = readSpawnIslandHome(worldDir);
		if (spawnIslandHome.isPresent()) {
			return spawnIslandHome.get();
		}

		if ("overworld".equals(dimensionKeyBeingReset)) {
			// The Overworld's own persisted spawn point is about to be regenerated too: fall back
			// to a fixed, always-in-bounds coordinate instead of soon-to-be-different terrain.
			return new RelocationTarget("minecraft:overworld", new BlockPos(0, 100, 0));
		}

		return new RelocationTarget("minecraft:overworld", readOverworldSpawn(worldDir.resolve("level.dat")));
	}

	// Reads the Spawn island's home location directly from its NBT file, mirroring
	// NbtIslandStorage's own schema (ownerUuid/dimension/homeLocation), instead of going through
	// IslandRegistry: this runs in onInitialize(), before the registry has loaded anything from disk.
	private static Optional<RelocationTarget> readSpawnIslandHome(Path worldDir) throws IOException {
		Path islandsDir = worldDir.resolve("islandcore").resolve("islands");
		if (!Files.isDirectory(islandsDir)) {
			return Optional.empty();
		}

		try (Stream<Path> files = Files.list(islandsDir)) {
			for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".dat")).toList()) {
				try {
					NbtCompound nbt = NbtIo.readCompressed(file, NbtSizeTracker.ofUnlimitedBytes());
					if (!Island.SERVER_OWNER_UUID.equals(nbt.getUuid("ownerUuid"))) {
						continue;
					}

					String dimensionId = nbt.getString("dimension");
					NbtCompound homeLocation = nbt.getCompound("homeLocation");
					BlockPos pos = new BlockPos(homeLocation.getInt("x"), homeLocation.getInt("y"), homeLocation.getInt("z"));
					return Optional.of(new RelocationTarget(dimensionId, pos));
				} catch (IOException | RuntimeException e) {
					IslandCoreMod.LOGGER.error("Failed to read island file {} while looking for the Spawn island, skipping", file, e);
				}
			}
		}

		return Optional.empty();
	}

	private static BlockPos readOverworldSpawn(Path levelDat) throws IOException {
		if (!Files.exists(levelDat)) {
			return new BlockPos(0, 100, 0);
		}

		NbtCompound root = NbtIo.readCompressed(levelDat, NbtSizeTracker.ofUnlimitedBytes());
		NbtCompound data = root.getCompound("Data");
		return new BlockPos(data.getInt("SpawnX"), data.getInt("SpawnY"), data.getInt("SpawnZ"));
	}

	// Only players whose last-saved "Dimension" tag matches the one being wiped are touched — a
	// safety net for anyone who logged in/out of that dimension between the admin's confirmation
	// and this boot (players connected AT confirmation time are already moved by
	// VanillaResetService, so this normally has nothing to do for them by the time this runs).
	private static void relocateOfflinePlayers(Path worldDir, String dimensionKeyBeingReset, RelocationTarget target) throws IOException {
		Path playerDataDir = worldDir.resolve("playerdata");
		if (!Files.isDirectory(playerDataDir)) {
			return;
		}

		String vanillaId = vanillaDimensionId(dimensionKeyBeingReset);

		try (Stream<Path> files = Files.list(playerDataDir)) {
			for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".dat")).toList()) {
				try {
					NbtCompound nbt = NbtIo.readCompressed(file, NbtSizeTracker.ofUnlimitedBytes());
					if (!vanillaId.equals(nbt.getString("Dimension"))) {
						continue;
					}

					nbt.putString("Dimension", target.dimensionId());
					NbtList pos = new NbtList();
					pos.add(NbtDouble.of(target.pos().getX() + 0.5));
					pos.add(NbtDouble.of(target.pos().getY()));
					pos.add(NbtDouble.of(target.pos().getZ() + 0.5));
					nbt.put("Pos", pos);

					NbtIo.writeCompressed(nbt, file);
				} catch (IOException | RuntimeException e) {
					IslandCoreMod.LOGGER.error("Failed to relocate player data file {}, leaving it as-is", file, e);
				}
			}
		}
	}

	private static String vanillaDimensionId(String dimensionKey) {
		return switch (dimensionKey) {
			case "overworld" -> "minecraft:overworld";
			case "nether" -> "minecraft:the_nether";
			case "end" -> "minecraft:the_end";
			default -> throw new IllegalArgumentException("Unknown vanilla dimension key: " + dimensionKey);
		};
	}

	private static String readLevelName(Path runDir) throws IOException {
		Path propertiesFile = runDir.resolve("server.properties");
		if (!Files.exists(propertiesFile)) {
			return "world";
		}

		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(propertiesFile, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}
		return properties.getProperty("level-name", "world");
	}

	// Overworld is the world's root save folder: it also holds level.dat, playerdata/, data/, and
	// the OTHER dimensions' subfolders (DIM-1, DIM1) — none of which a "reset overworld" request
	// may ever touch. Only its own terrain-cache subfolders are deleted here.
	private static Path[] foldersFor(String dimensionKey, Path worldDir) {
		return switch (dimensionKey) {
			case "overworld" -> new Path[] {
					worldDir.resolve("region"),
					worldDir.resolve("entities"),
					worldDir.resolve("poi")
			};
			case "nether" -> new Path[] { worldDir.resolve("DIM-1") };
			case "end" -> new Path[] { worldDir.resolve("DIM1") };
			default -> throw new IllegalArgumentException("Unknown vanilla dimension key: " + dimensionKey);
		};
	}

	private static void deleteRecursively(Path folder) throws IOException {
		if (!Files.exists(folder)) {
			return;
		}

		try (Stream<Path> walk = Files.walk(folder)) {
			walk.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.delete(path);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	private static void rewriteSeed(Path levelDat, long newSeed) throws IOException {
		NbtCompound root = NbtIo.readCompressed(levelDat, NbtSizeTracker.ofUnlimitedBytes());
		NbtCompound data = root.getCompound("Data");
		NbtCompound worldGenSettings = data.getCompound("WorldGenSettings");

		worldGenSettings.putLong("seed", newSeed);
		data.put("WorldGenSettings", worldGenSettings);
		root.put("Data", data);

		NbtIo.writeCompressed(root, levelDat);
	}
}
