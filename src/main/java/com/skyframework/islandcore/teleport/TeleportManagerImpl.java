package com.skyframework.islandcore.teleport;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.network.ActionOutcome;
import com.skyframework.islandcore.api.network.ActionReason;
import com.skyframework.islandcore.api.permission.IslandPermissions;
import com.skyframework.islandcore.rtp.SafeRandomTeleportFinder;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
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
			player.sendMessage(Text.literal("No tienes ninguna isla todavía."), false);
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		BlockPos home = island.getHomeLocation();
		if (home == null) {
			player.sendMessage(Text.literal("Tu isla no tiene un home asignado."), false);
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
					player.sendMessage(Text.literal(
							"Debes esperar " + remainingSeconds + " segundos más para volver a usar /island home."), false);
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
			player.sendMessage(Text.literal(
					"Tu home estaba fuera de los límites actuales de tu isla; se ha reajustado al centro."), false);
		}

		pending.put(playerUuid, new PendingTeleport(
				playerUuid, island.getDimension(), home, player.getPos(), HOME_WARMUP_TICKS, PendingTeleport.Kind.HOME));

		// Neutral confirmation only: the green countdown numbers (below, in tickAll) carry the
		// actual "X seconds left" information, starting almost immediately after this.
		player.sendMessage(Text.literal("Preparando teletransporte a tu isla. No te muevas ni recibas daño."), false);
		return ActionOutcome.ok();
	}

	@Override
	public ActionOutcome<Void> requestSpawn(ServerPlayerEntity player) {
		UUID playerUuid = player.getUuid();

		Optional<Island> maybeSpawnIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (maybeSpawnIsland.isEmpty()) {
			player.sendMessage(Text.literal("La isla de Spawn todavía no existe."), false);
			return ActionOutcome.fail(ActionReason.SPAWN_NOT_EXISTS);
		}

		Island spawnIsland = maybeSpawnIsland.get();
		BlockPos home = spawnIsland.getHomeLocation();

		if (!IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, IslandPermissions.SPAWN_COOLDOWN_BYPASS)) {
			Instant last = lastSpawnAt.get(playerUuid);
			if (last != null) {
				long cooldownMillis = IslandCoreMod.PERMISSION_PROVIDER.getSpawnCooldownSeconds(playerUuid) * 1000L;
				long elapsedMillis = Duration.between(last, Instant.now()).toMillis();
				if (elapsedMillis < cooldownMillis) {
					long remainingSeconds = (cooldownMillis - elapsedMillis + 999) / 1000;
					player.sendMessage(Text.literal(
							"Debes esperar " + remainingSeconds + " segundos más para volver a usar /spawn."), false);
					return ActionOutcome.fail(ActionReason.COOLDOWN_ACTIVE);
				}
			}
		}

		pending.put(playerUuid, new PendingTeleport(
				playerUuid, spawnIsland.getDimension(), home, player.getPos(), HOME_WARMUP_TICKS, PendingTeleport.Kind.SPAWN));

		player.sendMessage(Text.literal("Preparando teletransporte al spawn. No te muevas ni recibas daño."), false);
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
			player.sendMessage(Text.literal("La dimensión de farmeo no está disponible ahora mismo."), false);
			return ActionOutcome.fail(ActionReason.DIMENSION_UNAVAILABLE);
		}

		if (!IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, IslandPermissions.FARMING_COOLDOWN_BYPASS)) {
			Instant last = lastFarmingAt.get(playerUuid);
			if (last != null) {
				long cooldownMillis = IslandCoreMod.PERMISSION_PROVIDER.getFarmingCooldownSeconds(playerUuid) * 1000L;
				long elapsedMillis = Duration.between(last, Instant.now()).toMillis();
				if (elapsedMillis < cooldownMillis) {
					long remainingSeconds = (cooldownMillis - elapsedMillis + 999) / 1000;
					player.sendMessage(Text.literal(
							"Debes esperar " + remainingSeconds + " segundos más para volver a usar /farming."), false);
					return ActionOutcome.fail(ActionReason.COOLDOWN_ACTIVE);
				}
			}
		}

		pending.put(playerUuid, new PendingTeleport(
				playerUuid, targetDimension, world.getSpawnPos(), player.getPos(), HOME_WARMUP_TICKS, PendingTeleport.Kind.FARMING));

		player.sendMessage(Text.literal("Preparando teletransporte a la zona de farmeo. No te muevas ni recibas daño."), false);
		return ActionOutcome.ok();
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
					player.sendMessage(Text.literal(secondsRemaining + "...").formatted(Formatting.GREEN), false);
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
			player.sendMessage(Text.literal("No se ha podido completar el teletransporte: dimensión no disponible."), false);
			return;
		}

		boolean success = backend.teleport(player, world, teleport.targetPos);
		if (!success) {
			player.sendMessage(Text.literal("No se ha podido completar el teletransporte."), false);
			return;
		}

		if (teleport.kind == PendingTeleport.Kind.SPAWN) {
			lastSpawnAt.put(teleport.playerUuid, Instant.now());
			player.sendMessage(Text.literal("¡Teletransportado al spawn!"), false);
		} else if (teleport.kind == PendingTeleport.Kind.FARMING) {
			lastFarmingAt.put(teleport.playerUuid, Instant.now());
			player.sendMessage(Text.literal("¡Teletransportado a la zona de farmeo!"), false);
		} else {
			lastHomeAt.put(teleport.playerUuid, Instant.now());
			player.sendMessage(Text.literal("¡Teletransportado a tu isla!"), false);
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
			player.sendMessage(Text.literal("Teletransporte cancelado: " + reason + "."), false);
		}
	}

	private ServerPlayerEntity resolvePlayer(UUID playerUuid) {
		return server != null ? server.getPlayerManager().getPlayer(playerUuid) : null;
	}
}
