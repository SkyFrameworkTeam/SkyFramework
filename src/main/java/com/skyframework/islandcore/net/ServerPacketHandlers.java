package com.skyframework.islandcore.net;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.network.ActionOutcome;
import com.skyframework.islandcore.api.network.ActionReason;
import com.skyframework.islandcore.island.lifecycle.IslandActionService;
import com.skyframework.islandcore.island.lifecycle.MembershipService;
import com.skyframework.islandcore.island.model.IslandSetting;
import com.skyframework.islandcore.net.biome.BiomeTiersBuilder;
import com.skyframework.islandcore.net.biome.BiomeTiersRequestC2S;
import com.skyframework.islandcore.net.biome.BiomeTiersS2C;
import com.skyframework.islandcore.net.handshake.ClientHandshakeC2S;
import com.skyframework.islandcore.net.handshake.ServerHandshakeS2C;
import com.skyframework.islandcore.net.island.IslandBiomeChangeC2S;
import com.skyframework.islandcore.net.island.IslandCreateC2S;
import com.skyframework.islandcore.net.island.IslandDeleteConfirmC2S;
import com.skyframework.islandcore.net.island.IslandDeleteRequestC2S;
import com.skyframework.islandcore.net.island.IslandSettingsUpdateC2S;
import com.skyframework.islandcore.net.island.IslandSnapshotBuilder;
import com.skyframework.islandcore.net.island.IslandSnapshotRequestC2S;
import com.skyframework.islandcore.net.island.IslandSnapshotS2C;
import com.skyframework.islandcore.net.island.IslandUpgradeC2S;
import com.skyframework.islandcore.net.member.MemberInviteAcceptC2S;
import com.skyframework.islandcore.net.member.MemberInviteC2S;
import com.skyframework.islandcore.net.member.MemberRemoveC2S;
import com.skyframework.islandcore.net.member.MemberTrustC2S;
import com.skyframework.islandcore.net.teleport.TeleportRequestC2S;
import com.skyframework.islandcore.net.teleport.TeleportStatusBuilder;
import com.skyframework.islandcore.net.teleport.TeleportStatusRequestC2S;
import com.skyframework.islandcore.net.teleport.TeleportStatusS2C;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

public final class ServerPacketHandlers {

	private ServerPacketHandlers() {
	}

	public static void register() {
		registerPayloadTypes();

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

		registerIslandActionHandlers();
		registerMembershipHandlers();
		registerTeleportHandlers();
		registerBiomeTierHandlers();
	}

	private static void registerPayloadTypes() {
		PayloadTypeRegistry.playC2S().register(ClientHandshakeC2S.ID, ClientHandshakeC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(ServerHandshakeS2C.ID, ServerHandshakeS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(IslandSnapshotRequestC2S.ID, IslandSnapshotRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(IslandSnapshotS2C.ID, IslandSnapshotS2C.CODEC);

		PayloadTypeRegistry.playS2C().register(ActionResultS2C.ID, ActionResultS2C.CODEC);
		PayloadTypeRegistry.playS2C().register(PendingConfirmationTickS2C.ID, PendingConfirmationTickS2C.CODEC);

		PayloadTypeRegistry.playC2S().register(IslandCreateC2S.ID, IslandCreateC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(IslandUpgradeC2S.ID, IslandUpgradeC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(IslandDeleteRequestC2S.ID, IslandDeleteRequestC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(IslandDeleteConfirmC2S.ID, IslandDeleteConfirmC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(IslandSettingsUpdateC2S.ID, IslandSettingsUpdateC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(IslandBiomeChangeC2S.ID, IslandBiomeChangeC2S.CODEC);

		PayloadTypeRegistry.playC2S().register(MemberInviteC2S.ID, MemberInviteC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(MemberInviteAcceptC2S.ID, MemberInviteAcceptC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(MemberTrustC2S.ID, MemberTrustC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(MemberRemoveC2S.ID, MemberRemoveC2S.CODEC);

		PayloadTypeRegistry.playC2S().register(TeleportRequestC2S.ID, TeleportRequestC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportStatusRequestC2S.ID, TeleportStatusRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportStatusS2C.ID, TeleportStatusS2C.CODEC);

		PayloadTypeRegistry.playC2S().register(BiomeTiersRequestC2S.ID, BiomeTiersRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(BiomeTiersS2C.ID, BiomeTiersS2C.CODEC);
	}

	private static void registerIslandActionHandlers() {
		ServerPlayNetworking.registerGlobalReceiver(IslandCreateC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<?> outcome = IslandActionService.create(player, context.server());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		ServerPlayNetworking.registerGlobalReceiver(IslandUpgradeC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<?> outcome = IslandActionService.upgrade(player.getUuid());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		ServerPlayNetworking.registerGlobalReceiver(IslandDeleteRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<Void> outcome = IslandActionService.requestDelete(player.getUuid());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		ServerPlayNetworking.registerGlobalReceiver(IslandDeleteConfirmC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<Void> outcome = IslandActionService.confirmDelete(player.getUuid());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		ServerPlayNetworking.registerGlobalReceiver(IslandSettingsUpdateC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<Void> outcome = IslandSetting.fromId(payload.settingId())
					.map(setting -> IslandActionService.updateSetting(player.getUuid(), setting, payload.value()))
					.orElseGet(() -> ActionOutcome.fail(ActionReason.UNKNOWN_SETTING));
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		ServerPlayNetworking.registerGlobalReceiver(IslandBiomeChangeC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<?> outcome = parseBiomeId(payload.biomeId())
					.map(biomeId -> IslandActionService.changeBiome(player, biomeId, context.server()))
					.orElseGet(() -> ActionOutcome.fail(ActionReason.BIOME_NOT_FOUND));
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});
	}

	// Identifier.of(...) throws on a malformed string; the text command never hits this path
	// because IdentifierArgumentType already validates the format before executeBiome runs, but a
	// network payload's String field has no such brigadier-level guard.
	private static Optional<Identifier> parseBiomeId(String biomeId) {
		try {
			return Optional.of(Identifier.of(biomeId));
		} catch (RuntimeException e) {
			return Optional.empty();
		}
	}

	private static void registerMembershipHandlers() {
		ServerPlayNetworking.registerGlobalReceiver(MemberInviteC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<?> outcome = MembershipService.inviteByName(player, payload.targetName(), context.server());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		ServerPlayNetworking.registerGlobalReceiver(MemberInviteAcceptC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<?> outcome = MembershipService.acceptInvite(player, context.server());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		ServerPlayNetworking.registerGlobalReceiver(MemberTrustC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<Void> outcome = MembershipService.trust(player, payload.targetUuid());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		ServerPlayNetworking.registerGlobalReceiver(MemberRemoveC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<Void> outcome = MembershipService.removeMember(player, payload.targetUuid(), context.server());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});
	}

	private static void registerTeleportHandlers() {
		ServerPlayNetworking.registerGlobalReceiver(TeleportRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<?> outcome = dispatchTeleportRequest(player, payload.type(), context.server());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		ServerPlayNetworking.registerGlobalReceiver(TeleportStatusRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ServerPlayNetworking.send(player, TeleportStatusBuilder.build(player));
		});
	}

	// SPAWN/FARMING replicate the enabled-check SpawnCommand/FarmingCommand already do before
	// calling TeleportManager — that check lives in the text command today, not in
	// TeleportManagerImpl, so it must be repeated here for the network path to behave the same way.
	private static ActionOutcome<?> dispatchTeleportRequest(ServerPlayerEntity player, TeleportRequestC2S.Type type, MinecraftServer server) {
		return switch (type) {
			case HOME -> IslandCoreMod.TELEPORT_MANAGER.requestHome(player);
			case SPAWN -> IslandCoreMod.SPAWN_CONFIG.isEnabled()
					? IslandCoreMod.TELEPORT_MANAGER.requestSpawn(player)
					: ActionOutcome.fail(ActionReason.SPAWN_DISABLED);
			case FARMING -> IslandCoreMod.FARMING_CONFIG.isEnabled()
					? IslandCoreMod.TELEPORT_MANAGER.requestFarming(player)
					: ActionOutcome.fail(ActionReason.FARMING_DISABLED);
			case RTP -> IslandCoreMod.TELEPORT_MANAGER.requestRtp(player);
		};
	}

	private static void registerBiomeTierHandlers() {
		ServerPlayNetworking.registerGlobalReceiver(BiomeTiersRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ServerPlayNetworking.send(player, BiomeTiersBuilder.build(player));
		});
	}
}
