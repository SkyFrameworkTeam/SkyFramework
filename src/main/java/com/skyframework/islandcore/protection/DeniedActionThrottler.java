package com.skyframework.islandcore.protection;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Simple per-player+message cooldown for protection-denial messages (ProtectionListeners): the
// first denial for a given (player, exact message text) combination is sent immediately — no
// delay — then that exact combination goes quiet for COOLDOWN before it's allowed to announce
// again. A different denial reason (a different message, e.g. break vs. open-container) has its
// own independent cooldown and always announces immediately regardless of any other cooldown
// currently in progress for that player.
//
// Replaces an earlier "sliding window + xN summary" design: that one deferred the FIRST message
// until the window closed, leaving the player with no feedback at all while spamming the denied
// action — undesired. This is intentionally simpler: no counting, no summary, just silence during
// the cooldown.
public final class DeniedActionThrottler {

	private static final Duration COOLDOWN = Duration.ofSeconds(7);

	private record Key(UUID playerUuid, String message) {
	}

	private static final Map<Key, Instant> cooldownUntil = new ConcurrentHashMap<>();

	private DeniedActionThrottler() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> cooldownUntil.clear());
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID playerUuid = handler.getPlayer().getUuid();
			cooldownUntil.keySet().removeIf(key -> key.playerUuid().equals(playerUuid));
		});
	}

	// Called by ProtectionListeners instead of player.sendMessage(...) directly, for every
	// protection-denial message it sends. Takes the generic PlayerEntity type ProtectionListeners'
	// own Fabric event callbacks receive (always actually a ServerPlayerEntity by the time these
	// fire — callers already guard on `world instanceof ServerWorld` first).
	public static void notifyDenied(PlayerEntity player, Text message) {
		Key key = new Key(player.getUuid(), message.getString());
		Instant now = Instant.now();

		Instant until = cooldownUntil.get(key);
		if (until != null && now.isBefore(until)) {
			return;
		}

		player.sendMessage(message, false);
		cooldownUntil.put(key, now.plus(COOLDOWN));
	}
}
