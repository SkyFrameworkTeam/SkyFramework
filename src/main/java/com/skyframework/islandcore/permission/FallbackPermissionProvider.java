package com.skyframework.islandcore.permission;

import com.skyframework.islandcore.api.permission.IslandPermissions;
import com.skyframework.islandcore.api.permission.PermissionProvider;

import java.util.UUID;

// Used when LuckPerms isn't installed on the server.
public class FallbackPermissionProvider implements PermissionProvider {

	@Override
	public boolean hasPermission(UUID playerUuid, String permissionNode) {
		// Conservative default: deny everything.
		// TODO: this method only receives a UUID, so it can't check op status here. If the
		// fallback path ever needs to let ops bypass, the signature will need to change (or the
		// player will need to be resolved another way).
		return false;
	}

	@Override
	public int getHighestSizeAllowed(UUID playerUuid) {
		return IslandPermissions.DEFAULT_ISLAND_SIZE;
	}

	@Override
	public int getHomeCooldownSeconds(UUID playerUuid) {
		return IslandPermissions.DEFAULT_HOME_COOLDOWN_SECONDS;
	}

	@Override
	public long getBiomeCooldownSeconds(UUID playerUuid) {
		return IslandPermissions.DEFAULT_BIOME_COOLDOWN_SECONDS;
	}

	@Override
	public long getRtpCooldownSeconds(UUID playerUuid) {
		return IslandPermissions.DEFAULT_RTP_COOLDOWN_SECONDS;
	}

	@Override
	public long getSpawnCooldownSeconds(UUID playerUuid) {
		return IslandPermissions.DEFAULT_SPAWN_COOLDOWN_SECONDS;
	}

	@Override
	public long getFarmingCooldownSeconds(UUID playerUuid) {
		return IslandPermissions.DEFAULT_FARMING_COOLDOWN_SECONDS;
	}
}
