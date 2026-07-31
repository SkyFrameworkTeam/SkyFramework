package com.skyframework.islandcore.net;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.net.handshake.ClientHandshakeC2S;
import com.skyframework.islandcore.net.handshake.ServerHandshakeS2C;
import com.skyframework.islandcore.net.island.IslandSnapshotBuilder;
import com.skyframework.islandcore.net.island.IslandSnapshotRequestC2S;
import com.skyframework.islandcore.net.island.IslandSnapshotS2C;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.network.ServerPlayerEntity;

public final class ServerPacketHandlers {

	private ServerPacketHandlers() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(ClientHandshakeC2S.ID, ClientHandshakeC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(ServerHandshakeS2C.ID, ServerHandshakeS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(IslandSnapshotRequestC2S.ID, IslandSnapshotRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(IslandSnapshotS2C.ID, IslandSnapshotS2C.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ClientHandshakeC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ClientSyncNotifier.markHandshakeCompleted(player.getUuid());

			boolean isOperator = player.hasPermissionLevel(2);
			ServerPlayNetworking.send(player, new ServerHandshakeS2C(NetworkChannels.PROTOCOL_VERSION, isOperator));
		});

		ServerPlayNetworking.registerGlobalReceiver(IslandSnapshotRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			IslandSnapshotS2C snapshot = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid())
					.map(island -> IslandSnapshotBuilder.build(context.server(), island))
					.orElseGet(() -> IslandSnapshotBuilder.buildEmpty(player.getUuid()));

			ServerPlayNetworking.send(player, snapshot);
		});
	}
}
