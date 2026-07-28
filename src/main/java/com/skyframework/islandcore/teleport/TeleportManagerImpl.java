package com.skyframework.islandcore.teleport;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.permission.IslandPermissions;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

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
	private final Map<UUID, PendingTeleport> pending = new HashMap<>();
	private final Map<UUID, Instant> lastHomeAt = new HashMap<>();

	// onInitialize() runs before the MinecraftServer exists, so the reference is captured lazily on server start.
	private MinecraftServer server;

	public TeleportManagerImpl(TeleportBackend backend) {
		this.backend = backend;
		ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> this.server = startedServer);
	}

	@Override
	public void requestHome(ServerPlayerEntity player) {
		UUID playerUuid = player.getUuid();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(playerUuid);
		if (maybeIsland.isEmpty()) {
			player.sendMessage(Text.literal("No tienes ninguna isla todavía."), false);
			return;
		}

		Island island = maybeIsland.get();
		BlockPos home = island.getHomeLocation();
		if (home == null) {
			player.sendMessage(Text.literal("Tu isla no tiene un home asignado."), false);
			return;
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
					return;
				}
			}
		}

		pending.put(playerUuid, new PendingTeleport(playerUuid, island.getDimension(), home, player.getPos(), HOME_WARMUP_TICKS));

		// Neutral confirmation only: the green countdown numbers (below, in tickAll) carry the
		// actual "X seconds left" information, starting almost immediately after this.
		player.sendMessage(Text.literal("Preparando teletransporte a tu isla. No te muevas ni recibas daño."), false);
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
		if (success) {
			lastHomeAt.put(teleport.playerUuid, Instant.now());
			player.sendMessage(Text.literal("¡Teletransportado a tu isla!"), false);
		} else {
			player.sendMessage(Text.literal("No se ha podido completar el teletransporte."), false);
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
