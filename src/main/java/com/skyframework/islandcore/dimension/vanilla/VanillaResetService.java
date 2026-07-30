package com.skyframework.islandcore.dimension.vanilla;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.teleport.TeleportBackend;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

// Requests + confirms a vanilla dimension reset (Overworld/Nether/End), mirroring the same
// two-step-confirmation-with-countdown pattern as IslandDeletionServiceImpl/DimensionRegistryImpl.
//
// Unlike those, confirming here never touches a world file right away: the world for the
// requested dimension is still live and can't safely be torn down mid-session. Confirming only
// enqueues an entry into pending_vanilla_reset.json (a JSON array — several dimensions can be
// queued up before a single restart); the actual deletion/reseed happens from
// VanillaResetExecutor at the very start of the NEXT boot's onInitialize() (see Sprint 18
// research: that's the earliest point Fabric guarantees, well before level.dat is read that run).
public class VanillaResetService {

	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
	public static final Set<String> VALID_DIMENSION_KEYS = Set.of("overworld", "nether", "end");
	private static final String PENDING_FILE_NAME = "pending_vanilla_reset.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Map<String, PendingConfirmation> pendingConfirmations = new HashMap<>();
	private final TeleportBackend teleportBackend;

	// onInitialize() runs before the MinecraftServer exists, so the reference is captured lazily on server start.
	private MinecraftServer server;

	public VanillaResetService(TeleportBackend teleportBackend) {
		this.teleportBackend = teleportBackend;
		ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> this.server = startedServer);
	}

	// explicitSeed is captured here (not at confirm time) because the confirming command
	// ("vanilla regenerate <key> confirm") has no seed argument slot of its own — the admin can
	// only state it on this initial request, so it has to ride along inside the pending
	// confirmation until confirmReset() resolves it, same as DimensionRegistryImpl's
	// requestRegeneration/confirmRegeneration pair.
	public void requestReset(String dimensionKey, UUID requestedBy, Long explicitSeed) {
		requireValidKey(dimensionKey);
		pendingConfirmations.put(dimensionKey, new PendingConfirmation(requestedBy, Instant.now().plus(REQUEST_TIMEOUT), explicitSeed));
	}

	public boolean confirmReset(String dimensionKey, UUID requestedBy) {
		requireValidKey(dimensionKey);

		PendingConfirmation request = pendingConfirmations.get(dimensionKey);
		if (request == null) {
			return false;
		}

		if (Instant.now().isAfter(request.expiresAt)) {
			pendingConfirmations.remove(dimensionKey);
			return false;
		}

		if (!request.requestedBy.equals(requestedBy)) {
			return false;
		}

		pendingConfirmations.remove(dimensionKey);

		// Players still IN the dimension being wiped are moved out right away — the world itself
		// isn't touched until the next boot, but there's no reason to leave them sitting somewhere
		// that's about to be deleted for the remainder of this session.
		evacuateConnectedPlayers(dimensionKey);

		// Resolved now, not at next boot, so the applied seed doesn't depend on further randomness.
		Long seed = resolveSeed(request.explicitSeed);
		enqueuePendingReset(new PendingVanillaReset(
				dimensionKey, seed, requestedBy, PendingVanillaReset.Status.IN_PROGRESS, Instant.now()));
		return true;
	}

	// Cancels a QUEUED (already-confirmed) reset, not a PENDING_CONFIRMATION one still counting
	// down — that one is cancelled simply by letting the 30s window expire, same as every other
	// confirmation flow in this codebase.
	public boolean cancelPendingReset(String dimensionKey) {
		requireValidKey(dimensionKey);

		List<PendingVanillaReset> queue = readQueue();
		boolean removed = queue.removeIf(entry -> entry.getDimensionKey().equals(dimensionKey));
		if (!removed) {
			return false;
		}

		if (queue.isEmpty()) {
			try {
				Files.deleteIfExists(pendingFilePath());
			} catch (IOException e) {
				IslandCoreMod.LOGGER.error("Failed to delete {} after cancelling its last remaining entry", PENDING_FILE_NAME, e);
			}
		} else {
			writeQueue(queue);
		}

		return true;
	}

	public List<PendingVanillaReset> listPendingResets() {
		return readQueue();
	}

	public void tickAll() {
		if (pendingConfirmations.isEmpty() || server == null) {
			return;
		}

		Instant now = Instant.now();

		for (Iterator<Map.Entry<String, PendingConfirmation>> it = pendingConfirmations.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, PendingConfirmation> entry = it.next();
			String dimensionKey = entry.getKey();
			PendingConfirmation request = entry.getValue();

			if (now.isAfter(request.expiresAt)) {
				it.remove();
				notifyExpired(dimensionKey, request.requestedBy);
				continue;
			}

			long remainingMillis = Duration.between(now, request.expiresAt).toMillis();
			int secondsRemaining = (int) Math.max(1, (remainingMillis + 999) / 1000);

			if (secondsRemaining != request.lastNotifiedSecond) {
				request.lastNotifiedSecond = secondsRemaining;
				notifyCountdown(dimensionKey, request.requestedBy, secondsRemaining);
			}
		}
	}

	private Long resolveSeed(Long explicitSeed) {
		if (explicitSeed != null) {
			return explicitSeed;
		}

		if ("keep".equals(IslandCoreMod.VANILLA_RESET_CONFIG.getSeedMode())) {
			return null;
		}
		return new Random().nextLong();
	}

	// Replaces any existing queued entry for the same dimension (no point resetting it twice in the
	// same boot, and the newer request's seed choice should win) but leaves entries for other
	// dimensions untouched, so several resets can be queued up before a single restart.
	private void enqueuePendingReset(PendingVanillaReset pending) {
		List<PendingVanillaReset> queue = readQueue();
		queue.removeIf(entry -> entry.getDimensionKey().equals(pending.getDimensionKey()));
		queue.add(pending);
		writeQueue(queue);
	}

	// Shared with VanillaResetExecutor (same package), which checkpoints the queue by calling this
	// again with the not-yet-processed remainder after each entry it successfully applies.
	static List<PendingVanillaReset> readQueue() {
		List<PendingVanillaReset> queue = new ArrayList<>();

		Path file = pendingFilePath();
		if (!Files.exists(file)) {
			return queue;
		}

		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
			for (JsonElement element : array) {
				try {
					queue.add(PendingVanillaReset.fromJson(element.getAsJsonObject()));
				} catch (RuntimeException e) {
					IslandCoreMod.LOGGER.error("Skipping malformed entry in {}", PENDING_FILE_NAME, e);
				}
			}
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to read {}, starting a fresh queue", PENDING_FILE_NAME, e);
		}

		return queue;
	}

	static void writeQueue(List<PendingVanillaReset> queue) {
		JsonArray array = new JsonArray();
		for (PendingVanillaReset pending : queue) {
			array.add(pending.toJson());
		}

		Path file = pendingFilePath();
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(array));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to persist {}", PENDING_FILE_NAME, e);
		}
	}

	static Path pendingFilePath() {
		return FabricLoader.getInstance().getConfigDir().resolve("islandcore").resolve(PENDING_FILE_NAME);
	}

	private void evacuateConnectedPlayers(String dimensionKey) {
		if (server == null) {
			return;
		}

		RegistryKey<World> targetKey = vanillaKeyFor(dimensionKey);
		EvacuationTarget target = resolveEvacuationTarget(dimensionKey);
		if (target == null) {
			return;
		}

		for (ServerPlayerEntity player : List.copyOf(server.getPlayerManager().getPlayerList())) {
			if (player.getWorld().getRegistryKey().equals(targetKey)) {
				teleportBackend.teleport(player, target.world(), target.pos());
			}
		}
	}

	// Same Spawn-island-first, Overworld-spawn-fallback priority as
	// EvictionTargetResolver/DimensionRegistryImpl's own evacuation logic, with one addition: if
	// the dimension being wiped IS the Overworld and there's no Spawn island to fall back to, the
	// Overworld's own spawn point is about to be regenerated too, so a fixed always-in-bounds
	// coordinate is used instead of pointing the player at soon-to-be-different terrain.
	private EvacuationTarget resolveEvacuationTarget(String dimensionKeyBeingReset) {
		Optional<Island> spawnIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (spawnIsland.isPresent()) {
			Island spawn = spawnIsland.get();
			ServerWorld world = server.getWorld(spawn.getDimension());
			if (world != null) {
				return new EvacuationTarget(world, spawn.getHomeLocation());
			}
		}

		ServerWorld overworld = server.getWorld(World.OVERWORLD);
		if (overworld == null) {
			return null;
		}

		BlockPos pos = "overworld".equals(dimensionKeyBeingReset) ? new BlockPos(0, 100, 0) : overworld.getSpawnPos();
		return new EvacuationTarget(overworld, pos);
	}

	private static RegistryKey<World> vanillaKeyFor(String dimensionKey) {
		return switch (dimensionKey) {
			case "overworld" -> World.OVERWORLD;
			case "nether" -> World.NETHER;
			case "end" -> World.END;
			default -> throw new IllegalArgumentException("Unknown vanilla dimension key: " + dimensionKey);
		};
	}

	private record EvacuationTarget(ServerWorld world, BlockPos pos) {
	}

	private void requireValidKey(String dimensionKey) {
		if (!VALID_DIMENSION_KEYS.contains(dimensionKey)) {
			throw new IllegalArgumentException("Dimensión vanilla desconocida: " + dimensionKey + ". Usa overworld, nether o end.");
		}
	}

	private void notifyCountdown(String dimensionKey, UUID playerUuid, int secondsRemaining) {
		ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
		if (player == null) {
			return;
		}

		player.sendMessage(
				Text.literal(dimensionKey + " se reseteará en el próximo reinicio del servidor en " + secondsRemaining
						+ "s - /dimension vanilla regenerate " + dimensionKey + " confirm para confirmar")
						.formatted(Formatting.RED),
				true);
	}

	private void notifyExpired(String dimensionKey, UUID playerUuid) {
		ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
		if (player == null) {
			return;
		}

		player.sendMessage(Text.literal("La solicitud de reseteo de " + dimensionKey + " ha caducado."), false);
	}

	// lastNotifiedSecond tracks the countdown value last shown on the action bar, so
	// tickAll() only re-sends when the displayed second actually changes.
	private static final class PendingConfirmation {
		final UUID requestedBy;
		final Instant expiresAt;
		final Long explicitSeed;
		int lastNotifiedSecond = -1;

		PendingConfirmation(UUID requestedBy, Instant expiresAt, Long explicitSeed) {
			this.requestedBy = requestedBy;
			this.expiresAt = expiresAt;
			this.explicitSeed = explicitSeed;
		}
	}
}
