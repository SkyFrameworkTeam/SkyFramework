package com.skyframework.islandcore.net.teleport;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.network.ActionReason;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class TeleportStatusBuilder {

	private TeleportStatusBuilder() {
	}

	public static TeleportStatusS2C build(ServerPlayerEntity player) {
		UUID playerUuid = player.getUuid();

		// Unlike TeleportManagerImpl#requestHome (which only discovers this once the player
		// actually tries), the status the client uses to grey out the button ahead of time must
		// reflect it too — otherwise "home" always reports available even for a player with no
		// island yet.
		TeleportStatusS2C.StatusEntry home = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(playerUuid).isPresent()
				? TeleportStatusS2C.StatusEntry.available(IslandCoreMod.TELEPORT_MANAGER.getHomeCooldownRemainingSeconds(playerUuid))
				: TeleportStatusS2C.StatusEntry.unavailable(ActionReason.NO_ISLAND);

		TeleportStatusS2C.StatusEntry spawn = IslandCoreMod.SPAWN_CONFIG.isEnabled()
				? TeleportStatusS2C.StatusEntry.available(IslandCoreMod.TELEPORT_MANAGER.getSpawnCooldownRemainingSeconds(playerUuid))
				: TeleportStatusS2C.StatusEntry.unavailable(ActionReason.SPAWN_DISABLED);

		TeleportStatusS2C.StatusEntry farming = IslandCoreMod.FARMING_CONFIG.isEnabled()
				? TeleportStatusS2C.StatusEntry.available(IslandCoreMod.TELEPORT_MANAGER.getFarmingCooldownRemainingSeconds(playerUuid))
				: TeleportStatusS2C.StatusEntry.unavailable(ActionReason.FARMING_DISABLED);

		TeleportStatusS2C.StatusEntry rtp = buildRtpStatus(player, playerUuid);

		return new TeleportStatusS2C(home, spawn, rtp, farming);
	}

	private static TeleportStatusS2C.StatusEntry buildRtpStatus(ServerPlayerEntity player, UUID playerUuid) {
		if (!IslandCoreMod.RTP_CONFIG.isEnabled()) {
			return TeleportStatusS2C.StatusEntry.unavailable(ActionReason.RTP_DISABLED);
		}

		if (!IslandCoreMod.RTP_CONFIG.isAllowed(player.getServerWorld().getRegistryKey().getValue())) {
			return TeleportStatusS2C.StatusEntry.unavailable(ActionReason.RTP_DIMENSION_NOT_ALLOWED);
		}

		return TeleportStatusS2C.StatusEntry.available(IslandCoreMod.TELEPORT_MANAGER.getRtpCooldownRemainingSeconds(playerUuid));
	}
}
