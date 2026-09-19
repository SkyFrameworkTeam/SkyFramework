package com.skyframework.islandcore.dimension.registry;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.dimension.generation.EndSpawnPlatformGenerator;
import com.skyframework.islandcore.dimension.model.DimensionData;
import com.skyframework.islandcore.dimension.model.DimensionDefinition;
import com.skyframework.islandcore.dimension.model.DimensionGeneratorStyle;
import com.skyframework.islandcore.dimension.model.DimensionState;
import com.skyframework.islandcore.dimension.runtime.DimensionRuntimeProvider;
import com.skyframework.islandcore.dimension.storage.DimensionStorage;
import com.skyframework.islandcore.dimension.storage.NbtDimensionStorage;
import com.skyframework.islandcore.teleport.TeleportBackend;
import com.skyframework.islandcore.util.ServerLang;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Deliberately independent of the island/ package as a general rule: this is a standalone module,
// following the same registry+storage shape as IslandRegistryImpl but for dimensions.
//
// Eviction note — the ONE justified exception to that independence: resolveEvacuationTarget()
// below queries IslandCoreMod.ISLAND_REGISTRY (via the public api.island.Island interface only,
// never island.lifecycle internals) to prefer the Spawn island's home when evacuating players,
// mirroring the exact same priority island.lifecycle.EvictionTargetResolver already uses for
// kicks/island deletion. This keeps the evacuated-to location consistent across both systems
// instead of players landing somewhere different depending on which system evicted them. It's a
// single, narrow, read-only lookup — not a structural dependency — so it's accepted here rather
// than duplicating a whole shared module or leaving the inconsistency in place.
public class DimensionRegistryImpl implements DimensionRegistry {

	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

	private final Map<Identifier, DimensionData> dimensionsById = new HashMap<>();
	private final Map<Identifier, PendingConfirmation> pendingConfirmations = new HashMap<>();
	private final Map<Identifier, PendingRemoval> pendingRemovals = new HashMap<>();

	private final DimensionRuntimeProvider runtimeProvider;
	private final TeleportBackend teleportBackend;

	private DimensionStorage storage;

	// onInitialize() runs before the MinecraftServer exists, so the reference is captured lazily on server start.
	private MinecraftServer server;

	public DimensionRegistryImpl(DimensionRuntimeProvider runtimeProvider, TeleportBackend teleportBackend) {
		this.runtimeProvider = runtimeProvider;
		this.teleportBackend = teleportBackend;

		// Self-contained SERVER_STARTED/tick hooks, same pattern as IslandRegistryImpl — this
		// package can't share that listener since it must not import anything from island/.
		ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> {
			this.server = startedServer;
			initializeStorage(startedServer.getSavePath(WorldSavePath.ROOT));
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> tickAll());
	}

	public void initializeStorage(Path worldSaveDir) {
		storage = new NbtDimensionStorage(worldSaveDir.resolve("islandcore").resolve("dimensions"));

		Collection<DimensionData> loaded = storage.loadAll();

		for (DimensionData dimension : loaded) {
			dimensionsById.put(dimension.getId(), dimension);

			// This is the piece Multiworld/WorldManager don't guarantee reliably: every saved
			// dimension is re-materialized right here, on every boot, rather than leaving that up
			// to a separate step that might not run.
			if (server != null) {
				ServerWorld world = runtimeProvider.createOrLoadWorld(dimension, server);
				ensureEndSpawnPlatform(dimension, world);
			}
		}

		// Resume any deletion/regeneration that was mid-flight when the server last stopped. Must
		// run after the loop above so the dimension is actually loaded again first (deleteDimension
		// / regenerateDimension need a live world to evacuate/tear down).
		if (server == null) {
			return;
		}

		for (DimensionData dimension : loaded) {
			if (dimension.getState() == DimensionState.DELETING) {
				deleteDimension(dimension.getId());
			} else if (dimension.getState() == DimensionState.REGENERATING) {
				// The new seed is only persisted once the swap actually completes (see
				// finishRemoval), so a crash mid-regenerate always leaves the OLD seed still
				// stored. Regenerating with that same seed is safe and self-healing: it just
				// finishes tearing down and rebuilds an equivalent dimension, instead of leaving
				// it stuck in REGENERATING forever waiting for a manual admin regenerate.
				regenerateDimension(dimension.getId(), dimension.getSeed());
			}
		}
	}

	@Override
	public Optional<DimensionDefinition> getDimension(Identifier id) {
		return Optional.ofNullable(dimensionsById.get(id));
	}

	@Override
	public Collection<DimensionDefinition> getAllDimensions() {
		return List.copyOf(dimensionsById.values());
	}

	@Override
	public boolean exists(Identifier id) {
		return dimensionsById.containsKey(id);
	}

	@Override
	public DimensionDefinition createDimension(Identifier id, String displayName, DimensionGeneratorStyle style, long seed) {
		if (dimensionsById.containsKey(id)) {
			throw new IllegalStateException("A dimension with id " + id + " already exists");
		}

		Instant now = Instant.now();
		DimensionData dimension = new DimensionData(id, displayName, style, seed, DimensionState.ACTIVE, now, now);

		dimensionsById.put(id, dimension);
		saveIfStorageReady(dimension);

		if (server != null) {
			ServerWorld world = runtimeProvider.createOrLoadWorld(dimension, server);
			ensureEndSpawnPlatform(dimension, world);
		}

		return dimension;
	}

	@Override
	public void requestDeletion(Identifier id, UUID requestedBy) {
		DimensionData dimension = requireActive(id);
		pendingConfirmations.put(id, new PendingConfirmation(requestedBy, Instant.now().plus(REQUEST_TIMEOUT), false, 0L));
	}

	@Override
	public boolean confirmDeletion(Identifier id, UUID requestedBy) {
		return confirmPending(id, requestedBy, false);
	}

	@Override
	public void requestRegeneration(Identifier id, UUID requestedBy, long newSeed) {
		requireActive(id);
		pendingConfirmations.put(id, new PendingConfirmation(requestedBy, Instant.now().plus(REQUEST_TIMEOUT), true, newSeed));
	}

	@Override
	public boolean confirmRegeneration(Identifier id, UUID requestedBy) {
		return confirmPending(id, requestedBy, true);
	}

	@Override
	public void deleteDimension(Identifier id) {
		DimensionData dimension = dimensionsById.get(id);
		if (dimension == null) {
			return;
		}

		// Checkpoint first, before touching the world at all — mirrors IslandDeletionServiceImpl.
		dimension.setState(DimensionState.DELETING);
		saveIfStorageReady(dimension);

		if (server == null) {
			return;
		}

		evacuatePlayers(id, server);
		runtimeProvider.unloadAndDeleteWorld(id, server);
		pendingRemovals.put(id, new PendingRemoval(false, 0L));
	}

	@Override
	public void regenerateDimension(Identifier id, long newSeed) {
		DimensionData dimension = dimensionsById.get(id);
		if (dimension == null) {
			return;
		}

		dimension.setState(DimensionState.REGENERATING);
		saveIfStorageReady(dimension);

		if (server == null) {
			return;
		}

		evacuatePlayers(id, server);
		runtimeProvider.unloadAndDeleteWorld(id, server);
		pendingRemovals.put(id, new PendingRemoval(true, newSeed));
	}

	private DimensionData requireActive(Identifier id) {
		DimensionData dimension = dimensionsById.get(id);
		if (dimension == null) {
			throw new IllegalArgumentException("No existe ninguna dimensión gestionada con id " + id);
		}
		if (dimension.getState() != DimensionState.ACTIVE) {
			throw new IllegalStateException("La dimensión " + id + " ya está en proceso de " + dimension.getState() + ".");
		}
		return dimension;
	}

	private boolean confirmPending(Identifier id, UUID requestedBy, boolean expectRegenerate) {
		PendingConfirmation pending = pendingConfirmations.get(id);
		if (pending == null || pending.regenerate != expectRegenerate) {
			return false;
		}

		if (Instant.now().isAfter(pending.expiresAt)) {
			pendingConfirmations.remove(id);
			return false;
		}

		if (!pending.requestedBy.equals(requestedBy)) {
			return false;
		}

		pendingConfirmations.remove(id);
		if (pending.regenerate) {
			regenerateDimension(id, pending.newSeedIfRegenerating);
		} else {
			deleteDimension(id);
		}
		return true;
	}

	private void tickAll() {
		tickPendingConfirmations();
		tickPendingRemovals();
	}

	private void tickPendingConfirmations() {
		if (pendingConfirmations.isEmpty() || server == null) {
			return;
		}

		Instant now = Instant.now();

		for (Iterator<Map.Entry<Identifier, PendingConfirmation>> it = pendingConfirmations.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Identifier, PendingConfirmation> entry = it.next();
			Identifier id = entry.getKey();
			PendingConfirmation request = entry.getValue();

			if (now.isAfter(request.expiresAt)) {
				it.remove();
				notifyExpired(id, request.requestedBy);
				continue;
			}

			long remainingMillis = Duration.between(now, request.expiresAt).toMillis();
			int secondsRemaining = (int) Math.max(1, (remainingMillis + 999) / 1000);

			if (secondsRemaining != request.lastNotifiedSecond) {
				request.lastNotifiedSecond = secondsRemaining;
				notifyCountdown(id, request, secondsRemaining);
			}
		}
	}

	private void notifyCountdown(Identifier id, PendingConfirmation request, int secondsRemaining) {
		ServerPlayerEntity player = server.getPlayerManager().getPlayer(request.requestedBy);
		if (player == null) {
			return;
		}

		String action = request.regenerate ? "regenerará" : "eliminará";
		String actionEn = request.regenerate ? "will regenerate" : "will be deleted";
		String confirmCommand = request.regenerate ? "/dimension regenerate" : "/dimension delete";

		player.sendMessage(
				ServerLang.of(player,
								"Dimensión " + id.getPath() + " se " + action + " en " + secondsRemaining + "s - "
										+ confirmCommand + " " + id.getPath() + " confirm para confirmar",
								"Dimension " + id.getPath() + " " + actionEn + " in " + secondsRemaining + "s - "
										+ confirmCommand + " " + id.getPath() + " confirm to confirm")
						.copy().formatted(Formatting.RED),
				true);
	}

	private void notifyExpired(Identifier id, UUID playerUuid) {
		ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
		if (player == null) {
			return;
		}

		player.sendMessage(ServerLang.of(player,
				"La solicitud sobre la dimensión " + id.getPath() + " ha caducado.",
				"The request for dimension " + id.getPath() + " has expired."), false);
	}

	private void tickPendingRemovals() {
		if (pendingRemovals.isEmpty() || server == null) {
			return;
		}

		for (Iterator<Map.Entry<Identifier, PendingRemoval>> it = pendingRemovals.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Identifier, PendingRemoval> entry = it.next();
			Identifier id = entry.getKey();

			RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, id);
			if (server.getWorld(worldKey) != null) {
				// Fantasy is still waiting for the dimension to be empty of players/loaded chunks.
				continue;
			}

			it.remove();
			finishRemoval(id, entry.getValue());
		}
	}

	private void finishRemoval(Identifier id, PendingRemoval removal) {
		DimensionData dimension = dimensionsById.get(id);

		if (removal.regenerate && dimension != null) {
			dimension.setSeed(removal.newSeedIfRegenerating);
			dimension.setState(DimensionState.ACTIVE);
			saveIfStorageReady(dimension);
			ServerWorld world = runtimeProvider.createOrLoadWorld(dimension, server);
			ensureEndSpawnPlatform(dimension, world);
		} else {
			if (storage != null) {
				storage.delete(id);
			}
			dimensionsById.remove(id);
		}
	}

	private void evacuatePlayers(Identifier id, MinecraftServer server) {
		RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, id);

		Optional<EvacuationTarget> target = resolveEvacuationTarget(server);
		if (target.isEmpty()) {
			return;
		}

		for (ServerPlayerEntity player : List.copyOf(server.getPlayerManager().getPlayerList())) {
			if (player.getWorld().getRegistryKey().equals(worldKey)) {
				teleportBackend.teleport(player, target.get().world(), target.get().pos());
			}
		}
	}

	// See the class-level "Eviction note" comment for why this one lookup into island/ is accepted.
	private Optional<EvacuationTarget> resolveEvacuationTarget(MinecraftServer server) {
		Optional<Island> spawnIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (spawnIsland.isPresent()) {
			Island spawn = spawnIsland.get();
			ServerWorld world = server.getWorld(spawn.getDimension());
			if (world != null) {
				return Optional.of(new EvacuationTarget(world, spawn.getHomeLocation()));
			}
		}

		ServerWorld overworld = server.getOverworld();
		if (overworld == null) {
			return Optional.empty();
		}
		return Optional.of(new EvacuationTarget(overworld, overworld.getSpawnPos()));
	}

	private record EvacuationTarget(ServerWorld world, BlockPos pos) {
	}

	private void saveIfStorageReady(DimensionData dimension) {
		if (storage != null) {
			storage.save(dimension);
		}
	}

	// Called every time an END_LIKE dimension's world becomes live (fresh creation, every server
	// boot's re-materialization, and after a regeneration) rather than only once at creation — cheap
	// (9 blocks) and idempotent, so it also self-heals any pre-existing END_LIKE dimension that was
	// created before this platform existed, or one where the platform got griefed away. resolveSafeLanding's
	// own step1 (direct spawnPos check) then always succeeds immediately for world.getSpawnPos(), no
	// fallback search needed — the far-from-spawn "last known position" case is untouched by this.
	private void ensureEndSpawnPlatform(DimensionData dimension, ServerWorld world) {
		if (dimension.getGeneratorStyle() != DimensionGeneratorStyle.END_LIKE || world == null) {
			return;
		}

		BlockPos spawnPos = world.getSpawnPos();
		world.getChunk(spawnPos.getX() >> 4, spawnPos.getZ() >> 4);
		EndSpawnPlatformGenerator.generate(world, spawnPos);
	}

	// lastNotifiedSecond tracks the countdown value last shown on the action bar, so
	// tickPendingConfirmations() only re-sends when the displayed second actually changes.
	private static final class PendingConfirmation {
		final UUID requestedBy;
		final Instant expiresAt;
		final boolean regenerate;
		final long newSeedIfRegenerating;
		int lastNotifiedSecond = -1;

		PendingConfirmation(UUID requestedBy, Instant expiresAt, boolean regenerate, long newSeedIfRegenerating) {
			this.requestedBy = requestedBy;
			this.expiresAt = expiresAt;
			this.regenerate = regenerate;
			this.newSeedIfRegenerating = newSeedIfRegenerating;
		}
	}

	private static final class PendingRemoval {
		final boolean regenerate;
		final long newSeedIfRegenerating;

		PendingRemoval(boolean regenerate, long newSeedIfRegenerating) {
			this.regenerate = regenerate;
			this.newSeedIfRegenerating = newSeedIfRegenerating;
		}
	}
}
