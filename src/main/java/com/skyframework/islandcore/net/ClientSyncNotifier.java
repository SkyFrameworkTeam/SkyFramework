package com.skyframework.islandcore.net;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.net.island.IslandSnapshotBuilder;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

/**
 * Mechanism only for this sprint: nothing in IslandRegistry/DeletionService/etc. calls
 * {@link #notifyIslandChanged(UUID)} yet. It's wired up here so a future sprint (once more
 * mutation packets exist) can start calling it without adding new plumbing.
 */
public final class ClientSyncNotifier {

	// Tracks which connected players have completed the handshake, so a change made through some
	// other path (a command, an admin action) can still push a fresh snapshot to them.
	private static final Set<UUID> handshakeCompletedPlayers = ConcurrentHashMap.newKeySet();

	// Players whose ClientHandshakeC2S reported a protocolVersion that didn't match
	// NetworkChannels.PROTOCOL_VERSION. In-memory, per connection only (cleared on disconnect, same
	// as handshakeCompletedPlayers above) — a later reconnect (e.g. after updating the client mod)
	// gets a fresh handshake and isn't stuck incompatible forever. ServerPacketHandlers'
	// registerGuarded consults this to reject every subsequent packet from that connection, as a
	// fallback in case the client ignores ServerHandshakeS2C#protocolCompatible=false.
	private static final Set<UUID> protocolIncompatiblePlayers = ConcurrentHashMap.newKeySet();

	@Nullable
	private static MinecraftServer server;

	private ClientSyncNotifier() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> server = startedServer);
		ServerLifecycleEvents.SERVER_STOPPING.register(stoppingServer -> {
			server = null;
			handshakeCompletedPlayers.clear();
			protocolIncompatiblePlayers.clear();
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, disconnectedServer) -> {
			UUID playerUuid = handler.getPlayer().getUuid();
			handshakeCompletedPlayers.remove(playerUuid);
			protocolIncompatiblePlayers.remove(playerUuid);
		});
	}

	public static void markHandshakeCompleted(UUID playerUuid) {
		handshakeCompletedPlayers.add(playerUuid);
	}

	// Called by the handshake handler once per handshake, with whichever result it actually
	// computed — always one or the other, so a player can never be left over as incompatible from a
	// stale earlier handshake once a new one completes successfully.
	public static void markProtocolIncompatible(UUID playerUuid) {
		protocolIncompatiblePlayers.add(playerUuid);
	}

	public static void markProtocolCompatible(UUID playerUuid) {
		protocolIncompatiblePlayers.remove(playerUuid);
	}

	public static boolean isProtocolIncompatible(UUID playerUuid) {
		return protocolIncompatiblePlayers.contains(playerUuid);
	}

	// Not called by anything yet this sprint — see the class javadoc.
	public static void notifyIslandChanged(UUID islandId) {
		if (server == null) {
			return;
		}

		IslandCoreMod.ISLAND_REGISTRY.getIsland(islandId).ifPresent(island -> {
			UUID ownerUuid = island.getOwnerUuid();
			if (!handshakeCompletedPlayers.contains(ownerUuid)) {
				return;
			}

			ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerUuid);
			if (owner != null) {
				ServerPlayNetworking.send(owner, IslandSnapshotBuilder.build(server, ownerUuid, island));
			}
		});
	}
}
