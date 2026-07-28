package com.skyframework.islandcore.permission;

import com.skyframework.islandcore.api.permission.IslandPermissions;
import com.skyframework.islandcore.api.permission.PermissionProvider;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;

import java.util.Map;
import java.util.UUID;

// Only constructed when the "luckperms" mod is actually loaded (see IslandCoreMod.onInitialize()).
public class LuckPermsProvider implements PermissionProvider {

	@Override
	public boolean hasPermission(UUID playerUuid, String permissionNode) {
		User user = resolveUser(playerUuid);
		if (user == null) {
			// Player has never connected / doesn't exist in LuckPerms' storage.
			return false;
		}

		return user.getCachedData().getPermissionData().checkPermission(permissionNode).asBoolean();
	}

	@Override
	public int getHighestSizeAllowed(UUID playerUuid) {
		User user = resolveUser(playerUuid);
		if (user == null) {
			return IslandPermissions.DEFAULT_ISLAND_SIZE;
		}

		Map<String, Boolean> permissions = user.getCachedData().getPermissionData().getPermissionMap();

		int highest = IslandPermissions.DEFAULT_ISLAND_SIZE;
		boolean found = false;

		for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
			if (!entry.getValue()) {
				continue;
			}

			String node = entry.getKey();
			if (!node.startsWith(IslandPermissions.ISLAND_SIZE_NODE_PREFIX)) {
				continue;
			}

			String suffix = node.substring(IslandPermissions.ISLAND_SIZE_NODE_PREFIX.length());
			try {
				int size = Integer.parseInt(suffix);
				if (!found || size > highest) {
					highest = size;
					found = true;
				}
			} catch (NumberFormatException e) {
				// Malformed node (doesn't end in a valid integer): ignore silently.
			}
		}

		return highest;
	}

	@Override
	public int getHomeCooldownSeconds(UUID playerUuid) {
		User user = resolveUser(playerUuid);
		if (user == null) {
			return IslandPermissions.DEFAULT_HOME_COOLDOWN_SECONDS;
		}

		Map<String, Boolean> permissions = user.getCachedData().getPermissionData().getPermissionMap();

		int lowest = IslandPermissions.DEFAULT_HOME_COOLDOWN_SECONDS;
		boolean found = false;

		for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
			if (!entry.getValue()) {
				continue;
			}

			String node = entry.getKey();
			if (!node.startsWith(IslandPermissions.TELEPORT_COOLDOWN_NODE_PREFIX)) {
				continue;
			}

			String suffix = node.substring(IslandPermissions.TELEPORT_COOLDOWN_NODE_PREFIX.length());
			try {
				int seconds = Integer.parseInt(suffix);
				if (!found || seconds < lowest) {
					lowest = seconds;
					found = true;
				}
			} catch (NumberFormatException e) {
				// Malformed node (doesn't end in a valid integer, e.g. the "bypass" node
				// which shares this prefix): ignore silently.
			}
		}

		return lowest;
	}

	private User resolveUser(UUID playerUuid) {
		// Resolved lazily (not cached at construction/mod-init time): Fabric doesn't guarantee
		// that LuckPerms has finished its own initialization before ours runs, and
		// net.luckperms.api.LuckPermsProvider.get() throws NotLoadedException if called too early.
		// By the time a player actually triggers a permission check, the server has fully started.
		LuckPerms luckPerms = net.luckperms.api.LuckPermsProvider.get();
		UserManager userManager = luckPerms.getUserManager();

		User user = userManager.getUser(playerUuid);
		if (user == null) {
			// Not cached: block and load from storage. Acceptable for now since these
			// commands aren't high-frequency; can be optimized later if needed.
			user = userManager.loadUser(playerUuid).join();
		}

		return user;
	}
}
