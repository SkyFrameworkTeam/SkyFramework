package com.skyframework.islandcore.net;

import com.mojang.authlib.GameProfile;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.network.ActionOutcome;
import com.skyframework.islandcore.api.network.ActionReason;
import com.skyframework.islandcore.dimension.model.DimensionDefinition;
import com.skyframework.islandcore.dimension.model.DimensionGeneratorStyle;
import com.skyframework.islandcore.dimension.vanilla.PendingVanillaReset;
import com.skyframework.islandcore.island.lifecycle.IslandActionService;
import com.skyframework.islandcore.island.lifecycle.MembershipService;
import com.skyframework.islandcore.island.model.IslandMember;
import com.skyframework.islandcore.island.model.IslandRole;
import com.skyframework.islandcore.island.model.IslandSetting;
import com.skyframework.islandcore.party.lifecycle.PartyDisbandRequests;
import com.skyframework.islandcore.party.model.PartyData;
import com.skyframework.islandcore.net.alliance.AllyLocationsS2C;
import com.skyframework.islandcore.net.alliance.LocationSharingSetC2S;
import com.skyframework.islandcore.net.alliance.LocationSharingStatusRequestC2S;
import com.skyframework.islandcore.net.alliance.LocationSharingStatusS2C;
import com.skyframework.islandcore.net.admin.dimension.DimensionAdminBuilder;
import com.skyframework.islandcore.net.admin.dimension.DimensionCreateC2S;
import com.skyframework.islandcore.net.admin.dimension.DimensionDeleteC2S;
import com.skyframework.islandcore.net.admin.dimension.DimensionDeleteConfirmC2S;
import com.skyframework.islandcore.net.admin.dimension.DimensionDetailRequestC2S;
import com.skyframework.islandcore.net.admin.dimension.DimensionDetailS2C;
import com.skyframework.islandcore.net.admin.dimension.DimensionListRequestC2S;
import com.skyframework.islandcore.net.admin.dimension.DimensionListS2C;
import com.skyframework.islandcore.net.admin.dimension.DimensionRegenerateC2S;
import com.skyframework.islandcore.net.admin.dimension.DimensionRegenerateConfirmC2S;
import com.skyframework.islandcore.net.admin.island.AdminIslandBuilder;
import com.skyframework.islandcore.net.admin.island.AdminIslandDeleteC2S;
import com.skyframework.islandcore.net.admin.island.AdminIslandDeleteConfirmC2S;
import com.skyframework.islandcore.net.admin.island.AdminIslandDetailRequestC2S;
import com.skyframework.islandcore.net.admin.island.AdminIslandDetailS2C;
import com.skyframework.islandcore.net.admin.island.AdminIslandListRequestC2S;
import com.skyframework.islandcore.net.admin.island.AdminIslandListS2C;
import com.skyframework.islandcore.net.admin.spawn.SpawnAuthorizedPlayerAddC2S;
import com.skyframework.islandcore.net.admin.spawn.SpawnAuthorizedPlayerRemoveC2S;
import com.skyframework.islandcore.net.admin.spawn.SpawnBuildProtectionSetC2S;
import com.skyframework.islandcore.net.admin.spawn.SpawnBuildProtectionStatusRequestC2S;
import com.skyframework.islandcore.net.admin.spawn.SpawnBuildProtectionStatusS2C;
import com.skyframework.islandcore.net.admin.spawn.SpawnIslandCreateC2S;
import com.skyframework.islandcore.net.admin.spawn.SpawnIslandResizeC2S;
import com.skyframework.islandcore.net.admin.spawn.SpawnIslandSetHomeC2S;
import com.skyframework.islandcore.net.admin.spawn.SpawnStatusRequestC2S;
import com.skyframework.islandcore.net.admin.spawn.SpawnStatusS2C;
import com.skyframework.islandcore.net.admin.vanilla.VanillaResetCancelC2S;
import com.skyframework.islandcore.net.admin.vanilla.VanillaResetConfirmC2S;
import com.skyframework.islandcore.net.admin.vanilla.VanillaResetListRequestC2S;
import com.skyframework.islandcore.net.admin.vanilla.VanillaResetListS2C;
import com.skyframework.islandcore.net.admin.vanilla.VanillaResetQueueC2S;
import com.skyframework.islandcore.net.biome.BiomeTiersBuilder;
import com.skyframework.islandcore.net.biome.BiomeTiersRequestC2S;
import com.skyframework.islandcore.net.biome.BiomeTiersS2C;
import com.skyframework.islandcore.net.admin.defaults.AdminDefaultsBuilder;
import com.skyframework.islandcore.net.admin.defaults.AdminDefaultsStatusRequestC2S;
import com.skyframework.islandcore.net.admin.defaults.AdminDefaultsStatusS2C;
import com.skyframework.islandcore.net.admin.defaults.AdminExceptionSetServerDefaultC2S;
import com.skyframework.islandcore.net.admin.defaults.AdminFlagSetRequirementC2S;
import com.skyframework.islandcore.net.admin.defaults.AdminFlagSetServerDefaultC2S;
import com.skyframework.islandcore.net.flag.ExceptionGroupSetPresetC2S;
import com.skyframework.islandcore.net.flag.ExceptionGroupsStatusRequestC2S;
import com.skyframework.islandcore.net.flag.ExceptionGroupsStatusS2C;
import com.skyframework.islandcore.net.flag.FlagSetC2S;
import com.skyframework.islandcore.net.flag.FlagSetPresetC2S;
import com.skyframework.islandcore.net.flag.FlagsStatusBuilder;
import com.skyframework.islandcore.net.flag.FlagsStatusRequestC2S;
import com.skyframework.islandcore.net.flag.FlagsStatusS2C;
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
import com.skyframework.islandcore.net.member.MemberAllyAddC2S;
import com.skyframework.islandcore.net.member.MemberAllyRemoveC2S;
import com.skyframework.islandcore.net.member.MemberInviteAcceptC2S;
import com.skyframework.islandcore.net.member.MemberInviteDeclineC2S;
import com.skyframework.islandcore.net.member.MemberInviteC2S;
import com.skyframework.islandcore.net.member.MemberRemoveC2S;
import com.skyframework.islandcore.net.member.MemberTrustC2S;
import com.skyframework.islandcore.net.party.PartyAcceptC2S;
import com.skyframework.islandcore.net.party.PartyCreateC2S;
import com.skyframework.islandcore.net.party.PartyDisbandConfirmC2S;
import com.skyframework.islandcore.net.party.PartyDisbandRequestC2S;
import com.skyframework.islandcore.net.party.PartyInviteC2S;
import com.skyframework.islandcore.net.party.PartyKickC2S;
import com.skyframework.islandcore.net.party.PartyLeaveC2S;
import com.skyframework.islandcore.net.party.PartyRenameC2S;
import com.skyframework.islandcore.net.party.PartyStatusBuilder;
import com.skyframework.islandcore.net.party.PartyStatusRequestC2S;
import com.skyframework.islandcore.net.party.PartyStatusS2C;
import com.skyframework.islandcore.net.teleport.TeleportRequestC2S;
import com.skyframework.islandcore.net.teleport.TeleportStatusBuilder;
import com.skyframework.islandcore.net.teleport.TeleportStatusRequestC2S;
import com.skyframework.islandcore.net.teleport.TeleportStatusS2C;
import com.skyframework.islandcore.protection.exception.ExceptionGroup;
import com.skyframework.islandcore.protection.flag.Flag;
import com.skyframework.islandcore.protection.flag.FlagCategory;
import com.skyframework.islandcore.protection.flag.FlagPreset;
import com.skyframework.islandcore.protection.flag.FlagRegistry;
import com.skyframework.islandcore.protection.flag.TriState;
import com.skyframework.islandcore.util.ServerLang;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public final class ServerPacketHandlers {

	private ServerPacketHandlers() {
	}

	public static void register() {
		registerPayloadTypes();

		// Deliberately NOT routed through registerGuarded below: this is what DECIDES whether a
		// connection is protocol-compatible in the first place, so it must always be processed.
		ServerPlayNetworking.registerGlobalReceiver(ClientHandshakeC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ClientSyncNotifier.markHandshakeCompleted(player.getUuid());

			boolean protocolCompatible = payload.protocolVersion() == NetworkChannels.PROTOCOL_VERSION;
			if (protocolCompatible) {
				ClientSyncNotifier.markProtocolCompatible(player.getUuid());
			} else {
				ClientSyncNotifier.markProtocolIncompatible(player.getUuid());
			}

			boolean isOperator = player.hasPermissionLevel(2);
			ServerPlayNetworking.send(player, new ServerHandshakeS2C(NetworkChannels.PROTOCOL_VERSION, protocolCompatible, isOperator));
		});

		registerGuarded(IslandSnapshotRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			IslandSnapshotS2C snapshot = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid())
					.map(island -> IslandSnapshotBuilder.build(context.server(), player.getUuid(), island))
					.orElseGet(() -> IslandSnapshotBuilder.buildEmpty(context.server(), player.getUuid()));

			ServerPlayNetworking.send(player, snapshot);
		});

		registerIslandActionHandlers();
		registerMembershipHandlers();
		registerTeleportHandlers();
		registerBiomeTierHandlers();
		registerFlagExceptionHandlers();
		registerPartyHandlers();
		registerLocationSharingHandlers();

		registerAdminIslandHandlers();
		registerSpawnAdminHandlers();
		registerDimensionAdminHandlers();
		registerVanillaResetAdminHandlers();
		registerAdminDefaultsHandlers();
	}

	// Every C2S receiver except the handshake itself (see the comment above that registration)
	// routes through here: once a connection is marked protocol-incompatible (mismatched
	// PROTOCOL_VERSION reported at handshake time), every later packet from it is rejected with
	// ActionResultS2C.fail instead of reaching the real handler — a fallback in case the client
	// ignores ServerHandshakeS2C#protocolCompatible=false and keeps talking anyway. Sending a real
	// ActionResultS2C (rather than silently dropping) matters here specifically because some
	// callers are sitting in a client-side PendingActionTracker.await() — silence would leave that
	// waiting forever instead of surfacing a clear error.
	private static <T extends CustomPayload> void registerGuarded(CustomPayload.Id<T> id, ServerPlayNetworking.PlayPayloadHandler<T> handler) {
		ServerPlayNetworking.registerGlobalReceiver(id, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (ClientSyncNotifier.isProtocolIncompatible(player.getUuid())) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.PROTOCOL_MISMATCH));
				return;
			}
			handler.receive(payload, context);
		});
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
		PayloadTypeRegistry.playC2S().register(MemberInviteDeclineC2S.ID, MemberInviteDeclineC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(MemberTrustC2S.ID, MemberTrustC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(MemberRemoveC2S.ID, MemberRemoveC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(MemberAllyAddC2S.ID, MemberAllyAddC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(MemberAllyRemoveC2S.ID, MemberAllyRemoveC2S.CODEC);

		PayloadTypeRegistry.playC2S().register(TeleportRequestC2S.ID, TeleportRequestC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportStatusRequestC2S.ID, TeleportStatusRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportStatusS2C.ID, TeleportStatusS2C.CODEC);

		PayloadTypeRegistry.playC2S().register(BiomeTiersRequestC2S.ID, BiomeTiersRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(BiomeTiersS2C.ID, BiomeTiersS2C.CODEC);

		PayloadTypeRegistry.playC2S().register(FlagsStatusRequestC2S.ID, FlagsStatusRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(FlagsStatusS2C.ID, FlagsStatusS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(FlagSetC2S.ID, FlagSetC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(FlagSetPresetC2S.ID, FlagSetPresetC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(ExceptionGroupsStatusRequestC2S.ID, ExceptionGroupsStatusRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(ExceptionGroupsStatusS2C.ID, ExceptionGroupsStatusS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(ExceptionGroupSetPresetC2S.ID, ExceptionGroupSetPresetC2S.CODEC);

		PayloadTypeRegistry.playC2S().register(AdminDefaultsStatusRequestC2S.ID, AdminDefaultsStatusRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(AdminDefaultsStatusS2C.ID, AdminDefaultsStatusS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(AdminFlagSetServerDefaultC2S.ID, AdminFlagSetServerDefaultC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(AdminExceptionSetServerDefaultC2S.ID, AdminExceptionSetServerDefaultC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(AdminFlagSetRequirementC2S.ID, AdminFlagSetRequirementC2S.CODEC);

		PayloadTypeRegistry.playC2S().register(PartyStatusRequestC2S.ID, PartyStatusRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(PartyStatusS2C.ID, PartyStatusS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(PartyCreateC2S.ID, PartyCreateC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(PartyInviteC2S.ID, PartyInviteC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(PartyAcceptC2S.ID, PartyAcceptC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(PartyLeaveC2S.ID, PartyLeaveC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(PartyKickC2S.ID, PartyKickC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(PartyRenameC2S.ID, PartyRenameC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(PartyDisbandRequestC2S.ID, PartyDisbandRequestC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(PartyDisbandConfirmC2S.ID, PartyDisbandConfirmC2S.CODEC);

		PayloadTypeRegistry.playC2S().register(LocationSharingStatusRequestC2S.ID, LocationSharingStatusRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(LocationSharingStatusS2C.ID, LocationSharingStatusS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(LocationSharingSetC2S.ID, LocationSharingSetC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(AllyLocationsS2C.ID, AllyLocationsS2C.CODEC);

		PayloadTypeRegistry.playC2S().register(AdminIslandListRequestC2S.ID, AdminIslandListRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(AdminIslandListS2C.ID, AdminIslandListS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(AdminIslandDetailRequestC2S.ID, AdminIslandDetailRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(AdminIslandDetailS2C.ID, AdminIslandDetailS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(AdminIslandDeleteC2S.ID, AdminIslandDeleteC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(AdminIslandDeleteConfirmC2S.ID, AdminIslandDeleteConfirmC2S.CODEC);

		PayloadTypeRegistry.playC2S().register(SpawnStatusRequestC2S.ID, SpawnStatusRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(SpawnStatusS2C.ID, SpawnStatusS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(SpawnIslandCreateC2S.ID, SpawnIslandCreateC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(SpawnIslandResizeC2S.ID, SpawnIslandResizeC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(SpawnIslandSetHomeC2S.ID, SpawnIslandSetHomeC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(SpawnBuildProtectionStatusRequestC2S.ID, SpawnBuildProtectionStatusRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(SpawnBuildProtectionStatusS2C.ID, SpawnBuildProtectionStatusS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(SpawnBuildProtectionSetC2S.ID, SpawnBuildProtectionSetC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(SpawnAuthorizedPlayerAddC2S.ID, SpawnAuthorizedPlayerAddC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(SpawnAuthorizedPlayerRemoveC2S.ID, SpawnAuthorizedPlayerRemoveC2S.CODEC);

		PayloadTypeRegistry.playC2S().register(DimensionListRequestC2S.ID, DimensionListRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(DimensionListS2C.ID, DimensionListS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(DimensionDetailRequestC2S.ID, DimensionDetailRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(DimensionDetailS2C.ID, DimensionDetailS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(DimensionCreateC2S.ID, DimensionCreateC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(DimensionDeleteC2S.ID, DimensionDeleteC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(DimensionDeleteConfirmC2S.ID, DimensionDeleteConfirmC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(DimensionRegenerateC2S.ID, DimensionRegenerateC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(DimensionRegenerateConfirmC2S.ID, DimensionRegenerateConfirmC2S.CODEC);

		PayloadTypeRegistry.playC2S().register(VanillaResetListRequestC2S.ID, VanillaResetListRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(VanillaResetListS2C.ID, VanillaResetListS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(VanillaResetQueueC2S.ID, VanillaResetQueueC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(VanillaResetConfirmC2S.ID, VanillaResetConfirmC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(VanillaResetCancelC2S.ID, VanillaResetCancelC2S.CODEC);
	}

	private static void registerIslandActionHandlers() {
		registerGuarded(IslandCreateC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<?> outcome = IslandActionService.create(player, context.server());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(IslandUpgradeC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<?> outcome = IslandActionService.upgrade(player.getUuid());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(IslandDeleteRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<Void> outcome = IslandActionService.requestDelete(player.getUuid());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(IslandDeleteConfirmC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<Void> outcome = IslandActionService.confirmDelete(player.getUuid());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(IslandSettingsUpdateC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			// Same entry point the text command ("/island settings") calls: neither path decides on
			// its own whether a setting id maps to the new Flag system or the old IslandSetting one.
			ActionOutcome<Void> outcome = IslandActionService.updateLegacySetting(player.getUuid(), payload.settingId(), payload.value());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(IslandBiomeChangeC2S.ID, (payload, context) -> {
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
		registerGuarded(MemberInviteC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<?> outcome = MembershipService.inviteByName(player, payload.targetName(), context.server());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(MemberInviteAcceptC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<?> outcome = MembershipService.acceptInvite(player, context.server());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(MemberInviteDeclineC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<?> outcome = MembershipService.declineInvite(player, context.server());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		// Toggles the target between MEMBER and CO_OWNER depending on their current role — see
		// MembershipService#toggleCoOwner. The MembersScreen "Trust" button reflects this by showing
		// the current state and calling this same packet either direction.
		registerGuarded(MemberTrustC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<Void> outcome = MembershipService.toggleCoOwner(player, payload.targetUuid());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(MemberRemoveC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<Void> outcome = MembershipService.removeMember(player, payload.targetUuid(), context.server());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(MemberAllyAddC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();

			Optional<UUID> targetUuid = MembershipService.resolvePlayerUuid(payload.targetName(), context.server());
			if (targetUuid.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.TARGET_NOT_FOUND));
				return;
			}

			// Same entry point "/island ally add" calls.
			ActionOutcome<Void> outcome = MembershipService.allyAdd(player, targetUuid.get());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(MemberAllyRemoveC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			// Same entry point "/island ally remove" calls.
			ActionOutcome<Void> outcome = MembershipService.allyRemove(player, payload.targetUuid());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});
	}

	private static void registerTeleportHandlers() {
		registerGuarded(TeleportRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ActionOutcome<?> outcome = dispatchTeleportRequest(player, payload.type(), context.server());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(TeleportStatusRequestC2S.ID, (payload, context) -> {
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
		registerGuarded(BiomeTiersRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ServerPlayNetworking.send(player, BiomeTiersBuilder.build(player));
		});
	}

	// Player-facing (not admin-only), mirrors "/island flags"/"/island exceptions" exactly: every
	// handler below calls the same FlagResolver/ExceptionGroupRegistry/IslandActionService entry
	// points those text commands already use — see FlagsStatusBuilder for the read side.
	private static void registerFlagExceptionHandlers() {
		registerGuarded(FlagsStatusRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid());
			if (maybeIsland.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.NO_ISLAND));
				return;
			}

			ServerPlayNetworking.send(player, FlagsStatusBuilder.buildFlagsStatus(maybeIsland.get(), player.getUuid()));
		});

		registerGuarded(FlagSetC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();

			Optional<Flag> maybeFlag = FlagRegistry.get(payload.flagId());
			if (maybeFlag.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.FLAG_NOT_FOUND));
				return;
			}

			TriState value;
			try {
				value = TriState.valueOf(payload.value().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.INVALID_FLAG_VALUE));
				return;
			}

			// Same entry point "/island flags set" calls.
			ActionOutcome<Void> outcome = IslandActionService.updateFlag(player.getUuid(), maybeFlag.get(), value);
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(FlagSetPresetC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			// Same entry point "/island flags preset" calls — IslandActionService#applyFlagPreset
			// validates flagId/preset itself (via IslandRegistryApi#applyFlagPreset).
			ActionOutcome<Void> outcome = IslandActionService.applyFlagPreset(player.getUuid(), payload.flagId(), payload.preset());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(ExceptionGroupsStatusRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid());
			if (maybeIsland.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.NO_ISLAND));
				return;
			}

			ServerPlayNetworking.send(player, FlagsStatusBuilder.buildExceptionGroupsStatus(maybeIsland.get()));
		});

		registerGuarded(ExceptionGroupSetPresetC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();

			Optional<ExceptionGroup> maybeGroup = IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getGroup(payload.groupId());
			if (maybeGroup.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.EXCEPTION_GROUP_NOT_FOUND));
				return;
			}
			if (!maybeGroup.get().isOwnerConfigurable()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.EXCEPTION_GROUP_NOT_OWNER_CONFIGURABLE));
				return;
			}

			// Same entry point "/island exceptions preset" calls.
			ActionOutcome<Void> outcome = IslandActionService.applyExceptionGroupPreset(player.getUuid(), payload.groupId(), payload.preset());
			ServerPlayNetworking.send(player, ActionResultS2C.fromOutcome(outcome));
		});
	}

	// Admin-only: server-wide default configuration for ROLE_BASED flags and exception groups — see
	// AdminDefaultsStatusS2C's class javadoc for why ISLAND_GLOBAL flags aren't reachable here.
	private static void registerAdminDefaultsHandlers() {
		registerGuarded(AdminDefaultsStatusRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			ServerPlayNetworking.send(player, AdminDefaultsBuilder.build());
		});

		registerGuarded(AdminFlagSetServerDefaultC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Flag> maybeFlag = FlagRegistry.get(payload.flagId());
			if (maybeFlag.isEmpty() || maybeFlag.get().getCategory() != FlagCategory.ROLE_BASED) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.INVALID_FLAG_PRESET));
				return;
			}

			Optional<FlagPreset> preset = FlagPreset.fromId(payload.preset());
			if (preset.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.INVALID_FLAG_PRESET));
				return;
			}

			IslandCoreMod.SERVER_FLAG_DEFAULTS.setRoleBasedDefault(payload.flagId(), preset.get());
			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(AdminExceptionSetServerDefaultC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			if (IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getGroup(payload.groupId()).isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.INVALID_EXCEPTION_PRESET));
				return;
			}

			Optional<FlagPreset> preset = FlagPreset.fromId(payload.preset());
			if (preset.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.INVALID_EXCEPTION_PRESET));
				return;
			}

			IslandCoreMod.SERVER_EXCEPTION_DEFAULTS.setDefault(payload.groupId(), preset.get());
			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(AdminFlagSetRequirementC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			if (FlagRegistry.get(payload.flagId()).isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.FLAG_NOT_FOUND));
				return;
			}

			String node = payload.permissionNode().isEmpty() ? null : payload.permissionNode();
			IslandCoreMod.FLAG_PERMISSION_REQUIREMENTS.setRequiredPermission(payload.flagId(), node);
			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});
	}

	// Mirrors "/party" exactly: every handler below calls the same PartyRegistry/
	// PartyInviteManager/PartyDisbandRequests entry points that command tree already uses — see
	// PartyStatusBuilder for the read side.
	private static void registerPartyHandlers() {
		registerGuarded(PartyStatusRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			PartyStatusS2C response = IslandCoreMod.PARTY_REGISTRY.getPartyOf(player.getUuid())
					.map(party -> PartyStatusBuilder.build(context.server(), party))
					.orElseGet(() -> PartyStatusBuilder.buildAbsent(context.server(), player.getUuid()));
			ServerPlayNetworking.send(player, response);
		});

		registerGuarded(PartyCreateC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();

			// Pre-checked (read-only, same PartyRegistry accessors #createParty itself uses
			// internally) rather than just try/catching createParty's IllegalStateException: that
			// exception's message is the only way to tell "already in a party" apart from "name
			// taken", and matching on message text would be fragile — this way the client gets the
			// correct ActionReason for each cause instead of one guessed at random.
			if (IslandCoreMod.PARTY_REGISTRY.getPartyOf(player.getUuid()).isPresent()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.ALREADY_IN_PARTY));
				return;
			}
			if (IslandCoreMod.PARTY_REGISTRY.getPartyByName(payload.name()).isPresent()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.PARTY_NAME_TAKEN));
				return;
			}

			// Same entry point "/party create" calls.
			IslandCoreMod.PARTY_REGISTRY.createParty(payload.name(), player.getUuid());
			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(PartyInviteC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();

			Optional<PartyData> maybeParty = requirePartyLeader(player.getUuid());
			if (maybeParty.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(notLeaderReason(player.getUuid())));
				return;
			}
			PartyData party = maybeParty.get();

			Optional<UUID> targetUuid = MembershipService.resolvePlayerUuid(payload.targetName(), context.server());
			if (targetUuid.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.TARGET_NOT_FOUND));
				return;
			}

			// Same entry point "/party invite" calls — PartyInviteManager#requestInvite itself
			// throws if the target is already in a party (single possible cause here, unlike
			// createParty above, so a plain try/catch is precise enough).
			try {
				IslandCoreMod.PARTY_INVITE_MANAGER.requestInvite(party.getPartyId(), player.getUuid(), targetUuid.get());
			} catch (IllegalStateException e) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.ALREADY_IN_PARTY));
				return;
			}

			ServerPlayerEntity targetPlayer = context.server().getPlayerManager().getPlayer(targetUuid.get());
			if (targetPlayer != null) {
				targetPlayer.sendMessage(ServerLang.of(targetPlayer,
						player.getGameProfile().getName() + " te ha invitado a su party \"" + party.getName()
								+ "\". Usa /party accept en los próximos 5 minutos para unirte.",
						player.getGameProfile().getName() + " has invited you to their party \"" + party.getName()
								+ "\". Use /party accept within the next 5 minutes to join."), false);
			}

			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(PartyAcceptC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();

			Optional<PartyData> maybeParty;
			try {
				// Same entry point "/party accept" calls.
				maybeParty = IslandCoreMod.PARTY_INVITE_MANAGER.acceptInvite(player.getUuid());
			} catch (IllegalStateException e) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.ALREADY_IN_PARTY));
				return;
			}

			if (maybeParty.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.NO_PENDING_PARTY_INVITE));
				return;
			}
			PartyData party = maybeParty.get();

			ServerPlayerEntity leader = context.server().getPlayerManager().getPlayer(party.getLeaderUuid());
			if (leader != null) {
				leader.sendMessage(ServerLang.of(leader,
						player.getGameProfile().getName() + " ha aceptado tu invitación y se ha unido a la party.",
						player.getGameProfile().getName() + " has accepted your invitation and joined the party."), false);
			}

			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(PartyLeaveC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();

			if (IslandCoreMod.PARTY_REGISTRY.getPartyOf(player.getUuid()).isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.NO_PARTY));
				return;
			}

			// Same entry point "/party leave" calls.
			IslandCoreMod.PARTY_REGISTRY.leaveParty(player.getUuid());
			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(PartyKickC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();

			Optional<PartyData> maybeParty = requirePartyLeader(player.getUuid());
			if (maybeParty.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(notLeaderReason(player.getUuid())));
				return;
			}
			PartyData party = maybeParty.get();

			if (payload.targetUuid().equals(player.getUuid())) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.CANNOT_KICK_SELF));
				return;
			}
			if (!party.getMembers().contains(payload.targetUuid())) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.NOT_A_PARTY_MEMBER));
				return;
			}

			// Same entry point "/party kick" calls.
			IslandCoreMod.PARTY_REGISTRY.removeMember(party.getPartyId(), payload.targetUuid());

			ServerPlayerEntity targetPlayer = context.server().getPlayerManager().getPlayer(payload.targetUuid());
			if (targetPlayer != null) {
				targetPlayer.sendMessage(ServerLang.of(targetPlayer,
					"Has sido expulsado de la party \"" + party.getName() + "\".",
					"You've been kicked from the party \"" + party.getName() + "\"."), false);
			}

			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(PartyRenameC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();

			Optional<PartyData> maybeParty = requirePartyLeader(player.getUuid());
			if (maybeParty.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(notLeaderReason(player.getUuid())));
				return;
			}
			PartyData party = maybeParty.get();

			// Same reasoning as PartyCreateC2S above: pre-checked so the taken-name case gets its
			// own precise reason rather than a generic catch.
			Optional<PartyData> maybeExisting = IslandCoreMod.PARTY_REGISTRY.getPartyByName(payload.newName());
			if (maybeExisting.isPresent() && !maybeExisting.get().getPartyId().equals(party.getPartyId())) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.PARTY_NAME_TAKEN));
				return;
			}

			// Same entry point "/party rename" calls.
			IslandCoreMod.PARTY_REGISTRY.renameParty(party.getPartyId(), payload.newName());
			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(PartyDisbandRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();

			Optional<PartyData> maybeParty = requirePartyLeader(player.getUuid());
			if (maybeParty.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(notLeaderReason(player.getUuid())));
				return;
			}

			// Same 15s confirmation window "/party disband" arms.
			PartyDisbandRequests.request(maybeParty.get().getPartyId());
			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(PartyDisbandConfirmC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();

			Optional<PartyData> maybeParty = requirePartyLeader(player.getUuid());
			if (maybeParty.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(notLeaderReason(player.getUuid())));
				return;
			}
			PartyData party = maybeParty.get();

			if (!PartyDisbandRequests.confirm(party.getPartyId())) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.NO_PENDING_PARTY_DISBAND));
				return;
			}

			MinecraftServer server = context.server();
			for (UUID memberUuid : party.getMembers()) {
				if (memberUuid.equals(player.getUuid())) {
					continue;
				}
				ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberUuid);
				if (member != null) {
					member.sendMessage(ServerLang.of(member,
						"La party \"" + party.getName() + "\" ha sido disuelta por su líder.",
						"The party \"" + party.getName() + "\" has been disbanded by its leader."), false);
				}
			}

			// Same entry point "/party disband confirm" calls.
			IslandCoreMod.PARTY_REGISTRY.disbandParty(party.getPartyId());
			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

	}

	// Shared by every leader-only party handler above: returns the sender's party if they're its
	// leader, empty otherwise — caller then uses notLeaderReason(...) to report NO_PARTY vs
	// NOT_PARTY_LEADER. Mirrors PartyCommand#requireLeaderOf.
	private static Optional<PartyData> requirePartyLeader(UUID playerUuid) {
		Optional<PartyData> maybeParty = IslandCoreMod.PARTY_REGISTRY.getPartyOf(playerUuid);
		if (maybeParty.isEmpty() || !maybeParty.get().getLeaderUuid().equals(playerUuid)) {
			return Optional.empty();
		}
		return maybeParty;
	}

	private static String notLeaderReason(UUID playerUuid) {
		return IslandCoreMod.PARTY_REGISTRY.getPartyOf(playerUuid).isEmpty() ? ActionReason.NO_PARTY : ActionReason.NOT_PARTY_LEADER;
	}

	// Renamed from the old registerAllianceHandlers: the island-to-island alliance request/accept/
	// remove/status handlers that used to live here are gone (see AllianceService/AllianceRegistry,
	// both retired — individual-player alliance management now goes through MemberAllyAddC2S/
	// MemberAllyRemoveC2S instead, registered in the member/ block above). Only the location-sharing
	// toggles remain, now 4 independent booleans (party vs. allies, send vs. receive) instead of 2.
	private static void registerLocationSharingHandlers() {
		registerGuarded(LocationSharingStatusRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			boolean sendToParty = IslandCoreMod.LOCATION_SHARING_CONFIG.isSendPositionToPartyEnabled(player.getUuid());
			boolean receiveFromParty = IslandCoreMod.LOCATION_SHARING_CONFIG.isReceivePositionsFromPartyEnabled(player.getUuid());
			boolean sendToAllies = IslandCoreMod.LOCATION_SHARING_CONFIG.isSendPositionToAlliesEnabled(player.getUuid());
			boolean receiveFromAllies = IslandCoreMod.LOCATION_SHARING_CONFIG.isReceivePositionsFromAlliesEnabled(player.getUuid());
			ServerPlayNetworking.send(player, new LocationSharingStatusS2C(sendToParty, receiveFromParty, sendToAllies, receiveFromAllies));
		});

		registerGuarded(LocationSharingSetC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			IslandCoreMod.LOCATION_SHARING_CONFIG.setSendPositionToParty(player.getUuid(), payload.sendPositionToParty());
			IslandCoreMod.LOCATION_SHARING_CONFIG.setReceivePositionsFromParty(player.getUuid(), payload.receivePositionsFromParty());
			IslandCoreMod.LOCATION_SHARING_CONFIG.setSendPositionToAllies(player.getUuid(), payload.sendPositionToAllies());
			IslandCoreMod.LOCATION_SHARING_CONFIG.setReceivePositionsFromAllies(player.getUuid(), payload.receivePositionsFromAllies());
			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});
	}

	// Admin network block: island list/detail/delete, Spawn management, Dimension Manager, vanilla
	// reset queue. Every handler below starts with the same operator check (mirrors DimensionCommand
	// and IslandCommand's "admin" subtree, both .requires(source -> source.hasPermissionLevel(2))),
	// since a network payload has no Brigadier .requires(...) gate to reject a non-operator sender
	// before the handler even runs.

	private static final RegistryKey<World> ISLANDS_DIMENSION = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

	private static boolean rejectIfNotOperator(ServerPlayerEntity player) {
		if (player.hasPermissionLevel(2)) {
			return false;
		}
		ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.NOT_OPERATOR));
		return true;
	}

	private static void registerAdminIslandHandlers() {
		registerGuarded(AdminIslandListRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			AdminIslandListS2C response = AdminIslandBuilder.buildList(context.server(),
					IslandCoreMod.ISLAND_REGISTRY.getAllIslands(), payload.page(), payload.pageSize(), payload.searchQuery());
			ServerPlayNetworking.send(player, response);
		});

		registerGuarded(AdminIslandDetailRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(payload.targetUuid());
			if (maybeIsland.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.ISLAND_NOT_FOUND));
				return;
			}

			ServerPlayNetworking.send(player, AdminIslandBuilder.buildDetail(context.server(), maybeIsland.get()));
		});

		registerGuarded(AdminIslandDeleteC2S.ID, (payload, context) -> {
			ServerPlayerEntity admin = context.player();
			if (rejectIfNotOperator(admin)) {
				return;
			}

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(payload.targetUuid());
			if (maybeIsland.isEmpty()) {
				ServerPlayNetworking.send(admin, ActionResultS2C.fail(ActionReason.ISLAND_NOT_FOUND));
				return;
			}

			try {
				IslandCoreMod.DELETION_SERVICE.requestDeletion(maybeIsland.get().getIslandId(), admin.getUuid());
			} catch (IllegalStateException e) {
				ServerPlayNetworking.send(admin, ActionResultS2C.fail(ActionReason.ISLAND_ALREADY_DELETING));
				return;
			}

			ServerPlayNetworking.send(admin, ActionResultS2C.ok());
		});

		registerGuarded(AdminIslandDeleteConfirmC2S.ID, (payload, context) -> {
			ServerPlayerEntity admin = context.player();
			if (rejectIfNotOperator(admin)) {
				return;
			}

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(payload.targetUuid());
			if (maybeIsland.isEmpty()) {
				ServerPlayNetworking.send(admin, ActionResultS2C.fail(ActionReason.ISLAND_NOT_FOUND));
				return;
			}

			boolean confirmed = IslandCoreMod.DELETION_SERVICE.confirmDeletion(maybeIsland.get().getIslandId(), admin.getUuid());
			ServerPlayNetworking.send(admin, confirmed
					? ActionResultS2C.ok()
					: ActionResultS2C.fail(ActionReason.NO_PENDING_CONFIRMATION));
		});
	}

	private static void registerSpawnAdminHandlers() {
		registerGuarded(SpawnStatusRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			SpawnStatusS2C response = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID)
					.map(island -> new SpawnStatusS2C(true, island.getIslandSize(), Optional.of(island.getHomeLocation())))
					.orElseGet(SpawnStatusS2C::absent);
			ServerPlayNetworking.send(player, response);
		});

		registerGuarded(SpawnIslandCreateC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			try {
				IslandCoreMod.ISLAND_REGISTRY.createSpawnIsland(ISLANDS_DIMENSION, payload.size());
			} catch (IllegalStateException e) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.SPAWN_ISLAND_ALREADY_EXISTS));
				return;
			}

			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(SpawnIslandResizeC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
			if (maybeIsland.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.SPAWN_ISLAND_NOT_FOUND));
				return;
			}

			try {
				IslandCoreMod.ISLAND_REGISTRY.resizeIsland(maybeIsland.get().getIslandId(), payload.newSize());
			} catch (IllegalArgumentException e) {
				// resizeIsland only ever rejects for the same reason /island upgrade's own size cap
				// does (requested size doesn't fit the allowed/reserved plot) — no dedicated admin
				// key was specified for this, so this reuses the closest existing semantic match.
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.PLOT_SIZE_EXCEEDED));
				return;
			}

			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(SpawnIslandSetHomeC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
			if (maybeIsland.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.SPAWN_ISLAND_NOT_FOUND));
				return;
			}

			Island island = maybeIsland.get();
			BlockPos pos = player.getBlockPos();
			boolean inIslandsDimension = player.getWorld().getRegistryKey().equals(ISLANDS_DIMENSION);
			boolean withinBuiltIsland = island.getBounds().contains(pos);
			if (!inIslandsDimension || !withinBuiltIsland) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.UNSAFE_LOCATION));
				return;
			}

			IslandCoreMod.ISLAND_REGISTRY.updateHomeLocation(island.getIslandId(), pos);
			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(SpawnBuildProtectionStatusRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			SpawnBuildProtectionStatusS2C response = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID)
					.map(island -> new SpawnBuildProtectionStatusS2C(
							island.getSetting(IslandSetting.BUILD_PROTECTION),
							buildAuthorizedPlayers(context.server(), island)))
					.orElseGet(SpawnBuildProtectionStatusS2C::absent);
			ServerPlayNetworking.send(player, response);
		});

		registerGuarded(SpawnBuildProtectionSetC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
			if (maybeIsland.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.SPAWN_ISLAND_NOT_FOUND));
				return;
			}

			IslandCoreMod.ISLAND_REGISTRY.updateIslandSetting(
					maybeIsland.get().getIslandId(), IslandSetting.BUILD_PROTECTION, payload.enabled());
			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(SpawnAuthorizedPlayerAddC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
			if (maybeIsland.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.SPAWN_ISLAND_NOT_FOUND));
				return;
			}

			Optional<UUID> targetUuid = MembershipService.resolvePlayerUuid(payload.targetName(), context.server());
			if (targetUuid.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.TARGET_NOT_FOUND));
				return;
			}

			MembershipService.trustOnIsland(maybeIsland.get(), targetUuid.get());
			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(SpawnAuthorizedPlayerRemoveC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
			if (maybeIsland.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.SPAWN_ISLAND_NOT_FOUND));
				return;
			}

			MembershipService.untrustOnIsland(maybeIsland.get(), payload.targetUuid());
			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});
	}

	// Same MEMBER/CO_OWNER filter AdminIslandBuilder#countMembers and IslandSnapshotBuilder already
	// apply elsewhere: the owner (here, the synthetic Island.SERVER_OWNER_UUID — not a real player)
	// is never included.
	private static List<SpawnBuildProtectionStatusS2C.AuthorizedPlayerEntry> buildAuthorizedPlayers(MinecraftServer server, Island island) {
		List<SpawnBuildProtectionStatusS2C.AuthorizedPlayerEntry> entries = new ArrayList<>();
		for (IslandMember member : island.getMembers()) {
			if (member.role() != IslandRole.MEMBER && member.role() != IslandRole.CO_OWNER) {
				continue;
			}
			entries.add(new SpawnBuildProtectionStatusS2C.AuthorizedPlayerEntry(
					member.playerUuid(), resolvePlayerName(server, member.playerUuid()), member.role().name()));
		}
		return entries;
	}

	private static String resolvePlayerName(MinecraftServer server, UUID playerUuid) {
		return server.getUserCache().getByUuid(playerUuid).map(GameProfile::getName).orElse(playerUuid.toString());
	}

	private static void registerDimensionAdminHandlers() {
		registerGuarded(DimensionListRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			List<DimensionListS2C.DimensionEntry> entries = new ArrayList<>();
			for (DimensionDefinition dimension : IslandCoreMod.DIMENSION_REGISTRY.getAllDimensions()) {
				entries.add(DimensionAdminBuilder.buildEntry(dimension));
			}
			ServerPlayNetworking.send(player, new DimensionListS2C(entries));
		});

		registerGuarded(DimensionDetailRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Identifier> maybeId = parseDimensionId(payload.id());
			Optional<DimensionDefinition> maybeDimension = maybeId.flatMap(IslandCoreMod.DIMENSION_REGISTRY::getDimension);
			if (maybeDimension.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.DIMENSION_NOT_FOUND));
				return;
			}

			ServerPlayNetworking.send(player, DimensionAdminBuilder.buildDetail(maybeDimension.get()));
		});

		registerGuarded(DimensionCreateC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			DimensionGeneratorStyle style;
			try {
				style = DimensionGeneratorStyle.valueOf(payload.style().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.INVALID_STYLE));
				return;
			}

			Optional<Identifier> maybeId = parseDimensionId(payload.id());
			if (maybeId.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.INVALID_DIMENSION_ID));
				return;
			}

			long seed = payload.seed().orElseGet(() -> new Random().nextLong());

			try {
				IslandCoreMod.DIMENSION_REGISTRY.createDimension(maybeId.get(), payload.displayName(), style, seed);
			} catch (IllegalStateException e) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.DIMENSION_ALREADY_EXISTS));
				return;
			}

			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(DimensionDeleteC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Identifier> maybeId = parseDimensionId(payload.id());
			if (maybeId.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.INVALID_DIMENSION_ID));
				return;
			}

			try {
				IslandCoreMod.DIMENSION_REGISTRY.requestDeletion(maybeId.get(), player.getUuid());
			} catch (IllegalArgumentException e) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.DIMENSION_NOT_FOUND));
				return;
			} catch (IllegalStateException e) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.DIMENSION_STATE_CONFLICT));
				return;
			}

			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(DimensionDeleteConfirmC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Identifier> maybeId = parseDimensionId(payload.id());
			if (maybeId.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.INVALID_DIMENSION_ID));
				return;
			}

			boolean confirmed = IslandCoreMod.DIMENSION_REGISTRY.confirmDeletion(maybeId.get(), player.getUuid());
			ServerPlayNetworking.send(player, confirmed
					? ActionResultS2C.ok()
					: ActionResultS2C.fail(ActionReason.NO_PENDING_CONFIRMATION));
		});

		registerGuarded(DimensionRegenerateC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Identifier> maybeId = parseDimensionId(payload.id());
			if (maybeId.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.INVALID_DIMENSION_ID));
				return;
			}

			long newSeed = payload.seed().orElseGet(() -> new Random().nextLong());

			try {
				IslandCoreMod.DIMENSION_REGISTRY.requestRegeneration(maybeId.get(), player.getUuid(), newSeed);
			} catch (IllegalArgumentException e) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.DIMENSION_NOT_FOUND));
				return;
			} catch (IllegalStateException e) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.DIMENSION_STATE_CONFLICT));
				return;
			}

			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(DimensionRegenerateConfirmC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Identifier> maybeId = parseDimensionId(payload.id());
			if (maybeId.isEmpty()) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.INVALID_DIMENSION_ID));
				return;
			}

			boolean confirmed = IslandCoreMod.DIMENSION_REGISTRY.confirmRegeneration(maybeId.get(), player.getUuid());
			ServerPlayNetworking.send(player, confirmed
					? ActionResultS2C.ok()
					: ActionResultS2C.fail(ActionReason.NO_PENDING_CONFIRMATION));
		});
	}

	// Identifier.of(...) throws on a malformed string, same reasoning as parseBiomeId above: the
	// text command never hits this path because its Brigadier argument type already validates the
	// format, but a network payload's raw String field has no such guard.
	private static Optional<Identifier> parseDimensionId(String idPath) {
		try {
			return Optional.of(Identifier.of("islandcore", idPath));
		} catch (RuntimeException e) {
			return Optional.empty();
		}
	}

	private static void registerVanillaResetAdminHandlers() {
		registerGuarded(VanillaResetListRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			List<VanillaResetListS2C.QueueEntry> entries = new ArrayList<>();
			for (PendingVanillaReset pending : IslandCoreMod.VANILLA_RESET_SERVICE.listPendingResets()) {
				entries.add(new VanillaResetListS2C.QueueEntry(
						pending.getDimensionKey(), Optional.ofNullable(pending.getSeed()), pending.getSeedMode().name(),
						pending.getRequestedBy(), pending.getStatus().name()));
			}
			ServerPlayNetworking.send(player, new VanillaResetListS2C(entries));
		});

		registerGuarded(VanillaResetQueueC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			boolean alreadyQueued = IslandCoreMod.VANILLA_RESET_SERVICE.listPendingResets().stream()
					.anyMatch(entry -> entry.getDimensionKey().equals(payload.dimension()));
			if (alreadyQueued) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.VANILLA_RESET_ALREADY_QUEUED));
				return;
			}

			Long explicitSeed = "CUSTOM".equals(payload.seedMode()) ? payload.seedValue().orElse(null) : null;

			try {
				IslandCoreMod.VANILLA_RESET_SERVICE.requestReset(payload.dimension(), player.getUuid(), explicitSeed);
			} catch (IllegalArgumentException e) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.VANILLA_DIMENSION_INVALID));
				return;
			}

			ServerPlayNetworking.send(player, ActionResultS2C.ok());
		});

		registerGuarded(VanillaResetConfirmC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			boolean confirmed;
			try {
				confirmed = IslandCoreMod.VANILLA_RESET_SERVICE.confirmReset(payload.dimension(), player.getUuid());
			} catch (IllegalArgumentException e) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.VANILLA_DIMENSION_INVALID));
				return;
			}

			ServerPlayNetworking.send(player, confirmed
					? ActionResultS2C.ok()
					: ActionResultS2C.fail(ActionReason.NO_PENDING_CONFIRMATION));
		});

		registerGuarded(VanillaResetCancelC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			boolean cancelled;
			try {
				cancelled = IslandCoreMod.VANILLA_RESET_SERVICE.cancelPendingReset(payload.dimension());
			} catch (IllegalArgumentException e) {
				ServerPlayNetworking.send(player, ActionResultS2C.fail(ActionReason.VANILLA_DIMENSION_INVALID));
				return;
			}

			// Not the same 30s confirm window as NO_PENDING_CONFIRMATION's other uses (this cancels
			// an already-confirmed, QUEUED entry) but no dedicated key was specified for "nothing
			// queued to cancel", so this reuses the closest existing semantic match.
			ServerPlayNetworking.send(player, cancelled
					? ActionResultS2C.ok()
					: ActionResultS2C.fail(ActionReason.NO_PENDING_CONFIRMATION));
		});
	}
}
