package com.skyframework.islandcore.net.teleport;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.network.ActionReason;
import com.skyframework.islandcore.dimension.model.DimensionDefinition;
import com.skyframework.islandcore.dimension.model.DimensionState;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
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

		TeleportStatusS2C.StatusEntry rtp = buildRtpStatus(player, playerUuid);

		return new TeleportStatusS2C(home, spawn, rtp, buildDimensionEntries(playerUuid));
	}

	// One entry per ACTIVE DIMENSION_REGISTRY dimension (a REGENERATING/DELETING one is mid-flight,
	// not something to offer a teleport into right now). The entry matching FarmingConfig's own
	// target keeps mirroring that config's enabled flag/cooldown — see TeleportManager's javadoc on
	// requestDimensionTeleport; every other dimension is always enabled with no cooldown.
	private static List<TeleportStatusS2C.DimensionTeleportEntry> buildDimensionEntries(UUID playerUuid) {
		List<TeleportStatusS2C.DimensionTeleportEntry> entries = new ArrayList<>();

		for (DimensionDefinition dimension : IslandCoreMod.DIMENSION_REGISTRY.getAllDimensions()) {
			if (dimension.getState() != DimensionState.ACTIVE) {
				continue;
			}

			boolean isFarmingTarget = dimension.getId().equals(IslandCoreMod.FARMING_CONFIG.getTargetDimension());
			boolean enabled = !isFarmingTarget || IslandCoreMod.FARMING_CONFIG.isEnabled();
			long cooldownRemainingSeconds = enabled
					? IslandCoreMod.TELEPORT_MANAGER.getDimensionCooldownRemainingSeconds(playerUuid, dimension.getId())
					: 0;

			entries.add(new TeleportStatusS2C.DimensionTeleportEntry(
					dimension.getId().toString(), dimension.getDisplayName(), enabled, cooldownRemainingSeconds));
		}

		return entries;
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
