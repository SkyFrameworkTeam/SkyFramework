package com.skyframework.islandcore.protection;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Per-operator, per-connection toggle for /island admin override (see IslandCommand). While active
// for a given player, AccessControllerImpl treats them as OWNER on ANY island — Spawn included —
// for every permission, a full bypass rather than a role grant. In-memory only, same pattern as
// DeniedActionThrottler: cleared on disconnect, so it never survives a reconnect and the operator
// must re-enable it deliberately each session (deliberate design: not per-island, no auto-expiry).
public final class AdminOverrideState {

	private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

	private AdminOverrideState() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> ACTIVE.clear());
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ACTIVE.remove(handler.getPlayer().getUuid()));
	}

	public static boolean isActive(UUID playerUuid) {
		return ACTIVE.contains(playerUuid);
	}

	public static void setActive(UUID playerUuid, boolean active) {
		if (active) {
			ACTIVE.add(playerUuid);
		} else {
			ACTIVE.remove(playerUuid);
		}
	}
}
