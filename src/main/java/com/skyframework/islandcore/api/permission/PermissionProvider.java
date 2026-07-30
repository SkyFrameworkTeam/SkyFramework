package com.skyframework.islandcore.api.permission;

import java.util.UUID;

public interface PermissionProvider {

	boolean hasPermission(UUID playerUuid, String permissionNode);

	int getHighestSizeAllowed(UUID playerUuid);

	int getHomeCooldownSeconds(UUID playerUuid);

	long getBiomeCooldownSeconds(UUID playerUuid);

	long getRtpCooldownSeconds(UUID playerUuid);

	long getSpawnCooldownSeconds(UUID playerUuid);
}
