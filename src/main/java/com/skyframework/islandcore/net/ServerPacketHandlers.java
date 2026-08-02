package com.skyframework.islandcore.net;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.network.ActionOutcome;
import com.skyframework.islandcore.api.network.ActionReason;
import com.skyframework.islandcore.dimension.model.DimensionDefinition;
import com.skyframework.islandcore.dimension.model.DimensionGeneratorStyle;
import com.skyframework.islandcore.dimension.vanilla.PendingVanillaReset;
import com.skyframework.islandcore.island.lifecycle.IslandActionService;
import com.skyframework.islandcore.island.lifecycle.MembershipService;
import com.skyframework.islandcore.island.model.IslandSetting;
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

		registerAdminIslandHandlers();
		registerSpawnAdminHandlers();
		registerDimensionAdminHandlers();
		registerVanillaResetAdminHandlers();
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
		ServerPlayNetworking.registerGlobalReceiver(AdminIslandListRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			AdminIslandListS2C response = AdminIslandBuilder.buildList(context.server(),
					IslandCoreMod.ISLAND_REGISTRY.getAllIslands(), payload.page(), payload.pageSize(), payload.searchQuery());
			ServerPlayNetworking.send(player, response);
		});

		ServerPlayNetworking.registerGlobalReceiver(AdminIslandDetailRequestC2S.ID, (payload, context) -> {
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

		ServerPlayNetworking.registerGlobalReceiver(AdminIslandDeleteC2S.ID, (payload, context) -> {
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

		ServerPlayNetworking.registerGlobalReceiver(AdminIslandDeleteConfirmC2S.ID, (payload, context) -> {
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
		ServerPlayNetworking.registerGlobalReceiver(SpawnStatusRequestC2S.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			SpawnStatusS2C response = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID)
					.map(island -> new SpawnStatusS2C(true, island.getIslandSize(), Optional.of(island.getHomeLocation())))
					.orElseGet(SpawnStatusS2C::absent);
			ServerPlayNetworking.send(player, response);
		});

		ServerPlayNetworking.registerGlobalReceiver(SpawnIslandCreateC2S.ID, (payload, context) -> {
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

		ServerPlayNetworking.registerGlobalReceiver(SpawnIslandResizeC2S.ID, (payload, context) -> {
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

		ServerPlayNetworking.registerGlobalReceiver(SpawnIslandSetHomeC2S.ID, (payload, context) -> {
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
	}

	private static void registerDimensionAdminHandlers() {
		ServerPlayNetworking.registerGlobalReceiver(DimensionListRequestC2S.ID, (payload, context) -> {
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

		ServerPlayNetworking.registerGlobalReceiver(DimensionDetailRequestC2S.ID, (payload, context) -> {
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

		ServerPlayNetworking.registerGlobalReceiver(DimensionCreateC2S.ID, (payload, context) -> {
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

		ServerPlayNetworking.registerGlobalReceiver(DimensionDeleteC2S.ID, (payload, context) -> {
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

		ServerPlayNetworking.registerGlobalReceiver(DimensionDeleteConfirmC2S.ID, (payload, context) -> {
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

		ServerPlayNetworking.registerGlobalReceiver(DimensionRegenerateC2S.ID, (payload, context) -> {
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

		ServerPlayNetworking.registerGlobalReceiver(DimensionRegenerateConfirmC2S.ID, (payload, context) -> {
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
		ServerPlayNetworking.registerGlobalReceiver(VanillaResetListRequestC2S.ID, (payload, context) -> {
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

		ServerPlayNetworking.registerGlobalReceiver(VanillaResetQueueC2S.ID, (payload, context) -> {
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

		ServerPlayNetworking.registerGlobalReceiver(VanillaResetConfirmC2S.ID, (payload, context) -> {
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

		ServerPlayNetworking.registerGlobalReceiver(VanillaResetCancelC2S.ID, (payload, context) -> {
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
