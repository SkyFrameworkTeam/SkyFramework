package com.skyframework.islandcore.teleport;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.network.ActionOutcome;
import com.skyframework.islandcore.api.network.ActionReason;
import com.skyframework.islandcore.api.permission.IslandPermissions;
import com.skyframework.islandcore.dimension.model.DimensionDefinition;
import com.skyframework.islandcore.rtp.SafeRandomTeleportFinder;
import com.skyframework.islandcore.util.ServerLang;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TeleportManagerImpl implements TeleportManager {

	// TODO: make configurable once a general config system exists.
	private static final int HOME_WARMUP_TICKS = 60;

	// Deliberately its own constant, not SafeLocationFinder.DEFAULT_SEARCH_RADIUS (16, shared by
	// RTP/home/spawn/void rescue — left untouched): only resolveSafeLanding's dynamic-dimension-
	// landing search uses this. Raised from 256 to 1024 (Sprint "aterrizaje seguro" investigation,
	// part 2) after real captured data showed a repeat-visit anchor can sit in a void gap wider than
	// 256 blocks in every direction (findNearestSafeHeightAware's 45 real samples around (400,300) in
	// a test END_LIKE dimension all landed in void_air) — 1024 matches TheEndBiomeSource's own
	// documented "center" biome extent (see findNearestSafeHeightAware's own comment), giving the
	// search a real chance to reach the main island cluster from most anchors instead of only ever
	// covering a quarter of it. findNearestSafeHeightAware bounds its cost by attempt count (45 total),
	// not by columns-in-range, so this stays just as cheap as 256 was — same number of chunk
	// loads/heightmap checks either way, just spread across a wider area. This never fully guarantees a
	// hit (real void gaps can still exceed even this), which is exactly why the fallback chain below
	// (own home → Spawn home → inform without moving) exists as the last resort, not a bug to fix here.
	private static final int DIMENSION_LANDING_SEARCH_RADIUS = 1024;

	private static final double CANCEL_MOVE_DISTANCE = 0.5;

	private final TeleportBackend backend;
	private final SafeRandomTeleportFinder rtpFinder = new SafeRandomTeleportFinder();
	private final Map<UUID, PendingTeleport> pending = new HashMap<>();
	private final Map<UUID, Instant> lastHomeAt = new HashMap<>();
	// Independent from lastHomeAt: a player's /island home and /spawn cooldowns run separately.
	private final Map<UUID, Instant> lastSpawnAt = new HashMap<>();
	// Independent from the other two: /farming has its own cooldown.
	private final Map<UUID, Instant> lastFarmingAt = new HashMap<>();
	// Moved in from RtpCommand (Sprint "acciones de isla"): the cooldown state needs a single
	// shared home now that both the text command and the future network handler call requestRtp.
	private final Map<UUID, Instant> lastRtpAt = new HashMap<>();

	// onInitialize() runs before the MinecraftServer exists, so the reference is captured lazily on server start.
	private MinecraftServer server;

	public TeleportManagerImpl(TeleportBackend backend) {
		this.backend = backend;
		ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> this.server = startedServer);
	}

	@Override
	public ActionOutcome<Void> requestHome(ServerPlayerEntity player) {
		UUID playerUuid = player.getUuid();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(playerUuid);
		if (maybeIsland.isEmpty()) {
			player.sendMessage(ServerLang.of(player, "No tienes ninguna isla todavía.", "You don't have an island yet."), false);
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		BlockPos home = island.getHomeLocation();
		if (home == null) {
			player.sendMessage(ServerLang.of(player, "Tu isla no tiene un home asignado.", "Your island doesn't have a home set."), false);
			return ActionOutcome.fail(ActionReason.HOME_NOT_SET);
		}

		if (!IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, IslandPermissions.TELEPORT_COOLDOWN_BYPASS)) {
			Instant last = lastHomeAt.get(playerUuid);
			if (last != null) {
				// Re-evaluated on every call (not cached): the player's permissions can change
				// between one /island home attempt and the next.
				long cooldownMillis = IslandCoreMod.PERMISSION_PROVIDER.getHomeCooldownSeconds(playerUuid) * 1000L;
				long elapsedMillis = Duration.between(last, Instant.now()).toMillis();
				if (elapsedMillis < cooldownMillis) {
					long remainingSeconds = (cooldownMillis - elapsedMillis + 999) / 1000;
					player.sendMessage(ServerLang.of(player,
							"Debes esperar " + remainingSeconds + " segundos más para volver a usar /island home.",
							"You must wait " + remainingSeconds + " more seconds before using /island home again."), false);
					return ActionOutcome.fail(ActionReason.COOLDOWN_ACTIVE);
				}
			}
		}

		// Not reachable today through any normal path (resizeIsland only ever grows the bounds,
		// and home is always initialized to the island's center on creation), but defensive for
		// whatever shrinks/relocations a future sprint might add. Auto-correct rather than fail
		// the request outright, so the player isn't stuck unable to use /island home at all.
		if (!island.getBounds().contains(home)) {
			BlockPos center = island.getCenter();
			IslandCoreMod.ISLAND_REGISTRY.updateHomeLocation(island.getIslandId(), center);
			home = center;
			player.sendMessage(ServerLang.of(player,
					"Tu home estaba fuera de los límites actuales de tu isla; se ha reajustado al centro.",
					"Your home was outside your island's current bounds; it has been reset to the center."), false);
		}

		// Homes set before SafeLandingChecker existed (via /island sethome without validation), or
		// with a block broken out from under them since, may not have solid ground anymore —
		// teleporting there would drop the player into the void, which (if enabled) hands off to
		// VoidRescueListener; that used to fall back to this SAME unsafe point whenever it coincided
		// with the island's center (home defaults to center on creation, so breaking the ground
		// there breaks both at once), causing an actual infinite fall/rescue loop. Search outward
		// for the nearest safe spot instead of blindly trusting the center, and persist the fix so
		// this self-corrects once instead of re-running (and re-messaging the player) on every
		// future /island home.
		ServerWorld homeWorld = server != null ? server.getWorld(island.getDimension()) : null;
		if (homeWorld != null && !SafeLandingChecker.isSafe(homeWorld, home)) {
			BlockPos safe = SafeLocationFinder.findNearestSafe(homeWorld, home, SafeLocationFinder.DEFAULT_SEARCH_RADIUS, island.getBounds())
					.orElseGet(island::getCenter);
			IslandCoreMod.ISLAND_REGISTRY.updateHomeLocation(island.getIslandId(), safe);
			home = safe;
			player.sendMessage(ServerLang.of(player,
					"Tu home no tenía suelo seguro debajo; se ha reajustado a un punto seguro cercano.",
					"Your home had no safe ground beneath it; it has been reset to a nearby safe spot."), false);
		}

		pending.put(playerUuid, new PendingTeleport(
				playerUuid, island.getDimension(), home, player.getPos(), HOME_WARMUP_TICKS, PendingTeleport.Kind.HOME));

		// Neutral confirmation only: the green countdown numbers (below, in tickAll) carry the
		// actual "X seconds left" information, starting almost immediately after this.
		player.sendMessage(ServerLang.of(player, "Preparando teletransporte a tu isla. No te muevas ni recibas daño.", "Preparing teleport to your island. Don't move or take damage."), false);
		return ActionOutcome.ok();
	}

	@Override
	public ActionOutcome<Void> requestSpawn(ServerPlayerEntity player) {
		UUID playerUuid = player.getUuid();

		Optional<Island> maybeSpawnIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (maybeSpawnIsland.isEmpty()) {
			player.sendMessage(ServerLang.of(player, "La isla de Spawn todavía no existe.", "The Spawn island doesn't exist yet."), false);
			return ActionOutcome.fail(ActionReason.SPAWN_NOT_EXISTS);
		}

		Island spawnIsland = maybeSpawnIsland.get();
		BlockPos home = spawnIsland.getHomeLocation();

		// Same broken-block-under-home safety net as requestHome above — the Spawn island is
		// exactly as vulnerable to it (and, being everyone's fallback destination, arguably more
		// disruptive when it breaks), but had no validation here at all before this fix.
		ServerWorld spawnWorld = server != null ? server.getWorld(spawnIsland.getDimension()) : null;
		if (spawnWorld != null && home != null && !SafeLandingChecker.isSafe(spawnWorld, home)) {
			BlockPos safe = SafeLocationFinder.findNearestSafe(spawnWorld, home, SafeLocationFinder.DEFAULT_SEARCH_RADIUS, spawnIsland.getBounds())
					.orElseGet(spawnIsland::getCenter);
			IslandCoreMod.ISLAND_REGISTRY.updateHomeLocation(spawnIsland.getIslandId(), safe);
			home = safe;
			player.sendMessage(ServerLang.of(player,
					"El home de Spawn no tenía suelo seguro debajo; se ha reajustado a un punto seguro cercano.",
					"The Spawn home had no safe ground beneath it; it has been reset to a nearby safe spot."), false);
		}

		if (!IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, IslandPermissions.SPAWN_COOLDOWN_BYPASS)) {
			Instant last = lastSpawnAt.get(playerUuid);
			if (last != null) {
				long cooldownMillis = IslandCoreMod.PERMISSION_PROVIDER.getSpawnCooldownSeconds(playerUuid) * 1000L;
				long elapsedMillis = Duration.between(last, Instant.now()).toMillis();
				if (elapsedMillis < cooldownMillis) {
					long remainingSeconds = (cooldownMillis - elapsedMillis + 999) / 1000;
					player.sendMessage(ServerLang.of(player,
							"Debes esperar " + remainingSeconds + " segundos más para volver a usar /spawn.",
							"You must wait " + remainingSeconds + " more seconds before using /spawn again."), false);
					return ActionOutcome.fail(ActionReason.COOLDOWN_ACTIVE);
				}
			}
		}

		pending.put(playerUuid, new PendingTeleport(
				playerUuid, spawnIsland.getDimension(), home, player.getPos(), HOME_WARMUP_TICKS, PendingTeleport.Kind.SPAWN));

		player.sendMessage(ServerLang.of(player, "Preparando teletransporte al spawn. No te muevas ni recibas daño.", "Preparing teleport to spawn. Don't move or take damage."), false);
		return ActionOutcome.ok();
	}

	@Override
	public ActionOutcome<Void> requestFarming(ServerPlayerEntity player) {
		UUID playerUuid = player.getUuid();

		Identifier targetDimensionId = IslandCoreMod.FARMING_CONFIG.getTargetDimension();
		RegistryKey<World> targetDimension = RegistryKey.of(RegistryKeys.WORLD, targetDimensionId);

		// Resolved (and checked for availability) right away, unlike requestHome/requestSpawn:
		// their target position comes from the Island model regardless of whether the world is
		// currently loaded, but the farming destination IS the target world's own spawn point, so
		// there's nothing to compute without a live world to ask.
		ServerWorld world = server != null ? server.getWorld(targetDimension) : null;
		if (world == null) {
			player.sendMessage(ServerLang.of(player, "La dimensión de farmeo no está disponible ahora mismo.", "The farming dimension isn't available right now."), false);
			return ActionOutcome.fail(ActionReason.DIMENSION_UNAVAILABLE);
		}

		Optional<LandingResult> landing = resolveDynamicDimensionLanding(player, world, targetDimension, world.getSpawnPos());
		if (landing.isEmpty()) {
			player.sendMessage(ServerLang.of(player,
					"No se ha podido encontrar ningún lugar seguro donde teletransportarte.",
					"Couldn't find any safe location to teleport you to."), false);
			return ActionOutcome.fail(ActionReason.DIMENSION_NO_SAFE_LOCATION);
		}

		if (!IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, IslandPermissions.FARMING_COOLDOWN_BYPASS)) {
			Instant last = lastFarmingAt.get(playerUuid);
			if (last != null) {
				long cooldownMillis = IslandCoreMod.PERMISSION_PROVIDER.getFarmingCooldownSeconds(playerUuid) * 1000L;
				long elapsedMillis = Duration.between(last, Instant.now()).toMillis();
				if (elapsedMillis < cooldownMillis) {
					long remainingSeconds = (cooldownMillis - elapsedMillis + 999) / 1000;
					player.sendMessage(ServerLang.of(player,
							"Debes esperar " + remainingSeconds + " segundos más para volver a usar /farming.",
							"You must wait " + remainingSeconds + " more seconds before using /farming again."), false);
					return ActionOutcome.fail(ActionReason.COOLDOWN_ACTIVE);
				}
			}
		}

		LandingResult resolvedLanding = landing.get();
		pending.put(playerUuid, new PendingTeleport(
				playerUuid, resolvedLanding.dimension(), resolvedLanding.pos(), player.getPos(), HOME_WARMUP_TICKS,
				kindFor(resolvedLanding.fallback(), PendingTeleport.Kind.FARMING)));

		player.sendMessage(ServerLang.of(player, "Preparando teletransporte a la zona de farmeo. No te muevas ni recibas daño.", "Preparing teleport to the farming zone. Don't move or take damage."), false);
		return ActionOutcome.ok();
	}

	private static PendingTeleport.Kind kindFor(FallbackKind fallback, PendingTeleport.Kind targetKind) {
		return switch (fallback) {
			case TARGET -> targetKind;
			case OWN_HOME -> PendingTeleport.Kind.RESCUE_HOME;
			case SPAWN_HOME -> PendingTeleport.Kind.RESCUE_SPAWN;
		};
	}

	@Override
	public ActionOutcome<Void> requestDimensionTeleport(ServerPlayerEntity player, Identifier dimensionId) {
		// The farming dimension keeps its own cooldown/safe-landing behavior unchanged, whichever
		// DIMENSION_REGISTRY slot it happens to occupy — see the class javadoc on TeleportManager.
		if (dimensionId.equals(IslandCoreMod.FARMING_CONFIG.getTargetDimension())) {
			return requestFarming(player);
		}

		UUID playerUuid = player.getUuid();

		if (IslandCoreMod.DIMENSION_REGISTRY.getDimension(dimensionId).isEmpty()) {
			player.sendMessage(ServerLang.of(player, "Esa dimensión ya no existe.", "That dimension no longer exists."), false);
			return ActionOutcome.fail(ActionReason.DIMENSION_UNAVAILABLE);
		}

		RegistryKey<World> targetDimension = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
		ServerWorld world = server != null ? server.getWorld(targetDimension) : null;
		if (world == null) {
			player.sendMessage(ServerLang.of(player, "Esa dimensión no está disponible ahora mismo.", "That dimension isn't available right now."), false);
			return ActionOutcome.fail(ActionReason.DIMENSION_UNAVAILABLE);
		}

		// Second-or-later visit resumes where the player left off (heightmap/SafeLocationFinder as a
		// backup if the terrain there is no longer safe); a first visit has nothing stored yet, so
		// it falls back to the world's own spawn point, same as every other dimension's first visit.
		BlockPos anchor = IslandCoreMod.PLAYER_LAST_POSITION_STORE.getLastPosition(playerUuid, dimensionId)
				.orElseGet(world::getSpawnPos);
		Optional<LandingResult> landing = resolveDynamicDimensionLanding(player, world, targetDimension, anchor);
		if (landing.isEmpty()) {
			player.sendMessage(ServerLang.of(player,
					"No se ha podido encontrar ningún lugar seguro donde teletransportarte.",
					"Couldn't find any safe location to teleport you to."), false);
			return ActionOutcome.fail(ActionReason.DIMENSION_NO_SAFE_LOCATION);
		}

		// No cooldown for generic dynamic dimensions (per spec) — no Kind-specific lastXAt map to
		// check or update here, unlike HOME/SPAWN/FARMING above.
		LandingResult resolvedLanding = landing.get();
		pending.put(playerUuid, new PendingTeleport(
				playerUuid, resolvedLanding.dimension(), resolvedLanding.pos(), player.getPos(), HOME_WARMUP_TICKS,
				kindFor(resolvedLanding.fallback(), PendingTeleport.Kind.DIMENSION)));

		player.sendMessage(ServerLang.of(player, "Preparando teletransporte. No te muevas ni recibas daño.", "Preparing teleport. Don't move or take damage."), false);
		return ActionOutcome.ok();
	}

	// A column picked without any regard for what's actually there — world.getSpawnPos() for a
	// generated dimension, or a player's last known position in a dimension that may have
	// regenerated since — is never guaranteed to be safe to land on (that's exactly what players
	// were hitting: suffocating underground right after /island farming). Unlike requestHome/
	// requestSpawn (whose target is a specific, arbitrary block that can only ever lose its footing
	// one block at a time), targetPos here is a single fixed, known column, so first try correcting
	// straight up that column via the world's heightmap — same idiom SafeRandomTeleportFinder
	// already uses for /island rtp — before falling back to SafeLocationFinder's outward 2D search
	// (used by requestHome/requestSpawn) and, as a last resort, the untouched raw targetPos.
	//
	// Generalized from the farming-only resolveSafeFarmingSpawn(world) (Sprint "teletransportes
	// dinámicos"): the logic never actually depended on farming, only on "a single fixed column" —
	// now reused for both the farming dimension (still anchored to world.getSpawnPos(), unchanged)
	// and every other dynamic dimension (anchored to the player's last known position in it, or
	// world.getSpawnPos() on a first visit — see requestDimensionTeleport).
	// Returns empty only when NO safe spot could be found anywhere within the search radius of
	// targetPos — e.g. END_LIKE's world spawn falling in the void between islands with the nearest
	// island outside SafeLocationFinder's search radius, or a column that genuinely never generated
	// solid ground. Callers fall back further (see resolveDynamicDimensionLanding) instead of
	// silently teleporting into that emptiness the way this used to (returning targetPos unchanged).
	private Optional<BlockPos> resolveSafeLanding(ServerWorld world, BlockPos targetPos) {
		if (SafeLandingChecker.isSafe(world, targetPos)) {
			return Optional.of(targetPos);
		}

		world.getChunk(targetPos.getX() >> 4, targetPos.getZ() >> 4);
		int topY = SafeLocationFinder.resolveTopY(world, targetPos.getX(), targetPos.getZ());
		BlockPos aboveTerrain = new BlockPos(targetPos.getX(), topY, targetPos.getZ());
		if (SafeLandingChecker.isSafe(world, aboveTerrain)) {
			return Optional.of(aboveTerrain);
		}

		// aboveTerrain's Y is discarded here — findNearestSafeHeightAware re-samples the heightmap
		// per candidate column (see its own doc), so reusing a Y computed for THIS column would be
		// meaningless for any other one. Centering on targetPos's own X/Z is what actually matters.
		return SafeLocationFinder.findNearestSafeHeightAware(world, targetPos, DIMENSION_LANDING_SEARCH_RADIUS);
	}

	// Where FallbackKind.TARGET means "found near the dimension's own landing point" (the common
	// case) and OWN_HOME/SPAWN_HOME mean the safety net kicked in — see PendingTeleport.Kind's
	// RESCUE_HOME/RESCUE_SPAWN, which completeTeleport uses to message this accurately without
	// charging either cooldown for an involuntary rescue.
	private enum FallbackKind {
		TARGET, OWN_HOME, SPAWN_HOME
	}

	private record LandingResult(RegistryKey<World> dimension, BlockPos pos, FallbackKind fallback) {
	}

	// Tries resolveSafeLanding near targetPos first (the common case — nearby search only, no
	// dimension change). If that finds nothing at all, falls back in priority order to the player's
	// own island home, then the Spawn island's home, each only if it's still actually safe — same
	// priority VoidRescueListener already uses for its own out-of-world safety net. Empty only when
	// none of the three panned out; callers must not move the player in that case.
	private Optional<LandingResult> resolveDynamicDimensionLanding(
			ServerPlayerEntity player, ServerWorld world, RegistryKey<World> worldKey, BlockPos targetPos) {
		Optional<BlockPos> nearby = resolveSafeLanding(world, targetPos);
		if (nearby.isPresent()) {
			return Optional.of(new LandingResult(worldKey, nearby.get(), FallbackKind.TARGET));
		}

		Optional<BlockPos> ownHome = safeHomeLocation(IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid()));
		if (ownHome.isPresent()) {
			Island ownIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid()).orElseThrow();
			return Optional.of(new LandingResult(ownIsland.getDimension(), ownHome.get(), FallbackKind.OWN_HOME));
		}

		Optional<BlockPos> spawnHome = safeHomeLocation(IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID));
		if (spawnHome.isPresent()) {
			Island spawnIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID).orElseThrow();
			return Optional.of(new LandingResult(spawnIsland.getDimension(), spawnHome.get(), FallbackKind.SPAWN_HOME));
		}

		return Optional.empty();
	}

	// null/not-yet-loaded-world/unsafe home all resolve the same way here: "not a usable fallback" —
	// unlike requestHome/requestSpawn, this never self-corrects an unsafe home (that's a deliberate
	// action's job, not a fallback search's).
	private Optional<BlockPos> safeHomeLocation(Optional<Island> maybeIsland) {
		if (maybeIsland.isEmpty()) {
			return Optional.empty();
		}
		Island island = maybeIsland.get();
		BlockPos home = island.getHomeLocation();
		if (home == null) {
			return Optional.empty();
		}
		ServerWorld homeWorld = server != null ? server.getWorld(island.getDimension()) : null;
		if (homeWorld == null || !SafeLandingChecker.isSafe(homeWorld, home)) {
			return Optional.empty();
		}
		return Optional.of(home);
	}

	@Override
	public ActionOutcome<Long> requestRtp(ServerPlayerEntity player) {
		UUID playerUuid = player.getUuid();

		if (!IslandCoreMod.RTP_CONFIG.isEnabled()) {
			return ActionOutcome.fail(ActionReason.RTP_DISABLED);
		}

		ServerWorld world = player.getServerWorld();
		if (!IslandCoreMod.RTP_CONFIG.isAllowed(world.getRegistryKey().getValue())) {
			return ActionOutcome.fail(ActionReason.RTP_DIMENSION_NOT_ALLOWED);
		}

		if (!IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, IslandPermissions.RTP_COOLDOWN_BYPASS)) {
			Instant last = lastRtpAt.get(playerUuid);
			if (last != null) {
				long cooldownSeconds = IslandCoreMod.PERMISSION_PROVIDER.getRtpCooldownSeconds(playerUuid);
				Instant availableAt = last.plusSeconds(cooldownSeconds);
				if (Instant.now().isBefore(availableAt)) {
					long remainingSeconds = Duration.between(Instant.now(), availableAt).getSeconds();
					return new ActionOutcome<>(false, ActionReason.COOLDOWN_ACTIVE, remainingSeconds);
				}
			}
		}

		Optional<BlockPos> maybePos = rtpFinder.findSafeLocation(world, IslandCoreMod.RTP_CONFIG);
		if (maybePos.isEmpty()) {
			// No cooldown applied: don't penalize the player for bad luck finding a spot.
			return ActionOutcome.fail(ActionReason.RTP_NO_SAFE_LOCATION);
		}

		backend.teleport(player, world, maybePos.get());
		lastRtpAt.put(playerUuid, Instant.now());

		return ActionOutcome.ok();
	}

	@Override
	public long getHomeCooldownRemainingSeconds(UUID playerUuid) {
		return remainingCooldownSeconds(playerUuid, lastHomeAt, IslandPermissions.TELEPORT_COOLDOWN_BYPASS,
				IslandCoreMod.PERMISSION_PROVIDER.getHomeCooldownSeconds(playerUuid));
	}

	@Override
	public long getSpawnCooldownRemainingSeconds(UUID playerUuid) {
		return remainingCooldownSeconds(playerUuid, lastSpawnAt, IslandPermissions.SPAWN_COOLDOWN_BYPASS,
				IslandCoreMod.PERMISSION_PROVIDER.getSpawnCooldownSeconds(playerUuid));
	}

	@Override
	public long getFarmingCooldownRemainingSeconds(UUID playerUuid) {
		return remainingCooldownSeconds(playerUuid, lastFarmingAt, IslandPermissions.FARMING_COOLDOWN_BYPASS,
				IslandCoreMod.PERMISSION_PROVIDER.getFarmingCooldownSeconds(playerUuid));
	}

	@Override
	public long getRtpCooldownRemainingSeconds(UUID playerUuid) {
		return remainingCooldownSeconds(playerUuid, lastRtpAt, IslandPermissions.RTP_COOLDOWN_BYPASS,
				IslandCoreMod.PERMISSION_PROVIDER.getRtpCooldownSeconds(playerUuid));
	}

	@Override
	public long getDimensionCooldownRemainingSeconds(UUID playerUuid, Identifier dimensionId) {
		if (dimensionId.equals(IslandCoreMod.FARMING_CONFIG.getTargetDimension())) {
			return getFarmingCooldownRemainingSeconds(playerUuid);
		}
		return 0;
	}

	private static long remainingCooldownSeconds(UUID playerUuid, Map<UUID, Instant> lastAt, String bypassPermission, long cooldownSeconds) {
		if (IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, bypassPermission)) {
			return 0;
		}

		Instant last = lastAt.get(playerUuid);
		if (last == null) {
			return 0;
		}

		Instant availableAt = last.plusSeconds(cooldownSeconds);
		Instant now = Instant.now();
		return now.isBefore(availableAt) ? Duration.between(now, availableAt).getSeconds() : 0;
	}

	@Override
	public void tickAll() {
		if (pending.isEmpty()) {
			return;
		}

		for (UUID playerUuid : List.copyOf(pending.keySet())) {
			PendingTeleport teleport = pending.get(playerUuid);
			if (teleport == null) {
				continue;
			}

			ServerPlayerEntity player = resolvePlayer(playerUuid);
			if (player == null) {
				// Disconnected mid-warmup: nothing left to notify.
				pending.remove(playerUuid);
				continue;
			}

			if (player.getPos().distanceTo(teleport.startPosition) > CANCEL_MOVE_DISTANCE) {
				cancelPendingTeleport(playerUuid, "te has movido");
				continue;
			}

			teleport.ticksRemaining--;

			if (teleport.ticksRemaining > 0) {
				if (teleport.ticksRemaining % 20 == 0) {
					int secondsRemaining = teleport.ticksRemaining / 20;
					player.sendMessage(ServerLang.of(player, secondsRemaining + "...", secondsRemaining + "...").copy().formatted(Formatting.GREEN), false);
				}
				continue;
			}

			pending.remove(playerUuid);
			completeTeleport(player, teleport);
		}
	}

	private void completeTeleport(ServerPlayerEntity player, PendingTeleport teleport) {
		ServerWorld world = server != null ? server.getWorld(teleport.targetDimension) : null;
		if (world == null) {
			player.sendMessage(ServerLang.of(player, "No se ha podido completar el teletransporte: dimensión no disponible.", "Couldn't complete the teleport: dimension not available."), false);
			return;
		}

		boolean success = backend.teleport(player, world, teleport.targetPos);
		if (!success) {
			player.sendMessage(ServerLang.of(player, "No se ha podido completar el teletransporte.", "Couldn't complete the teleport."), false);
			return;
		}

		switch (teleport.kind) {
			case SPAWN -> {
				lastSpawnAt.put(teleport.playerUuid, Instant.now());
				player.sendMessage(ServerLang.of(player, "¡Teletransportado al spawn!", "Teleported to spawn!"), false);
			}
			case FARMING -> {
				lastFarmingAt.put(teleport.playerUuid, Instant.now());
				player.sendMessage(ServerLang.of(player, "¡Teletransportado a la zona de farmeo!", "Teleported to the farming zone!"), false);
			}
			case DIMENSION -> {
				// No cooldown map to update (see requestDimensionTeleport) — displayName is re-fetched
				// here rather than stored on PendingTeleport, same "resolve fresh, don't cache"
				// approach the rest of this class already takes with island/dimension state.
				String displayName = IslandCoreMod.DIMENSION_REGISTRY.getDimension(teleport.targetDimension.getValue())
						.map(DimensionDefinition::getDisplayName)
						.orElse(teleport.targetDimension.getValue().getPath());
				player.sendMessage(ServerLang.of(player, "¡Teletransportado a " + displayName + "!", "Teleported to " + displayName + "!"), false);
			}
			// No cooldown charged for either rescue kind — this wasn't the deliberate action its
			// cooldown protects, the target dimension simply had nowhere safe to land at all (see
			// resolveDynamicDimensionLanding) — same "safety net, not a chosen action" reasoning
			// VoidRescueListener already uses.
			case RESCUE_HOME -> player.sendMessage(ServerLang.of(player,
					"No se encontró ningún lugar seguro en el destino; se te ha teletransportado a tu isla en su lugar.",
					"No safe spot was found at the destination; you were teleported to your island instead."), false);
			case RESCUE_SPAWN -> player.sendMessage(ServerLang.of(player,
					"No se encontró ningún lugar seguro en el destino; se te ha teletransportado al spawn en su lugar.",
					"No safe spot was found at the destination; you were teleported to spawn instead."), false);
			case HOME -> {
				lastHomeAt.put(teleport.playerUuid, Instant.now());
				player.sendMessage(ServerLang.of(player, "¡Teletransportado a tu isla!", "Teleported to your island!"), false);
			}
		}
	}

	@Override
	public void cancelPendingTeleport(UUID playerUuid, String reason) {
		PendingTeleport removed = pending.remove(playerUuid);
		if (removed == null) {
			return;
		}

		ServerPlayerEntity player = resolvePlayer(playerUuid);
		if (player != null) {
			player.sendMessage(ServerLang.of(player, "Teletransporte cancelado: " + reason + ".", "Teleport cancelled: " + reason + "."), false);
		}
	}

	private ServerPlayerEntity resolvePlayer(UUID playerUuid) {
		return server != null ? server.getPlayerManager().getPlayer(playerUuid) : null;
	}
}
