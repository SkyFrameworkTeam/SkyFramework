package com.skyframework.islandcore.net.island;

import com.mojang.authlib.GameProfile;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.island.entity.EntityCategory;
import com.skyframework.islandcore.island.lifecycle.PendingInvite;
import com.skyframework.islandcore.island.model.IslandMember;
import com.skyframework.islandcore.island.model.IslandRole;
import com.skyframework.islandcore.island.model.IslandSetting;

import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Pure assembly: every field below comes straight from an existing service (IslandRegistry via
// the Island object itself, PermissionProvider, InviteManager, IslandEntityTracker). No business
// logic lives here, only mapping domain state onto the network wire format.
public final class IslandSnapshotBuilder {

	private IslandSnapshotBuilder() {
	}

	public static IslandSnapshotS2C build(MinecraftServer server, Island island) {
		int maxSize = IslandCoreMod.PERMISSION_PROVIDER.getHighestSizeAllowed(island.getOwnerUuid());

		List<IslandSnapshotS2C.MemberEntry> members = new ArrayList<>();
		members.add(new IslandSnapshotS2C.MemberEntry(
				island.getOwnerUuid(), resolveName(server, island.getOwnerUuid()), IslandRole.OWNER.name()));
		for (IslandMember member : island.getMembers()) {
			// Mirrors IslandMessages' member sections: VISITOR/DENIED aren't shown as members.
			if (member.role() != IslandRole.MEMBER && member.role() != IslandRole.TRUSTED) {
				continue;
			}
			members.add(new IslandSnapshotS2C.MemberEntry(
					member.playerUuid(), resolveName(server, member.playerUuid()), member.role().name()));
		}

		List<IslandSnapshotS2C.PendingInviteEntry> pendingInvites = new ArrayList<>();
		for (PendingInvite invite : IslandCoreMod.INVITE_MANAGER.getPendingInvitesForIsland(island.getIslandId())) {
			long remainingSeconds = Math.max(0L, Duration.between(Instant.now(), invite.expiresAt()).getSeconds());
			pendingInvites.add(new IslandSnapshotS2C.PendingInviteEntry(
					resolveName(server, invite.inviteeUuid()), (int) remainingSeconds));
		}

		List<IslandSnapshotS2C.SettingEntry> settings = new ArrayList<>();
		for (IslandSetting setting : IslandSetting.values()) {
			settings.add(new IslandSnapshotS2C.SettingEntry(setting.name(), island.getSetting(setting)));
		}

		Map<EntityCategory, Integer> counts = IslandCoreMod.ENTITY_TRACKER.countByCategory(island.getIslandId());
		IslandSnapshotS2C.EntityCounts entities = new IslandSnapshotS2C.EntityCounts(
				counts.getOrDefault(EntityCategory.PLAYERS, 0),
				counts.getOrDefault(EntityCategory.HOSTILE, 0),
				counts.getOrDefault(EntityCategory.PASSIVE, 0),
				counts.getOrDefault(EntityCategory.COBBLEMON, 0),
				counts.getOrDefault(EntityCategory.ITEMS, 0),
				counts.getOrDefault(EntityCategory.OTHER, 0)
		);

		return new IslandSnapshotS2C(
				true,
				island.getIslandSize(),
				maxSize,
				island.getIslandType().getId(),
				island.getCurrentBiomeId(),
				Optional.of(island.getHomeLocation()),
				island.getState().name(),
				members,
				pendingInvites,
				settings,
				entities
		);
	}

	// maxSize still comes from PermissionProvider even without an island yet, so the client can
	// show what a future island's cap would be.
	public static IslandSnapshotS2C buildEmpty(UUID playerUuid) {
		int maxSize = IslandCoreMod.PERMISSION_PROVIDER.getHighestSizeAllowed(playerUuid);
		return new IslandSnapshotS2C(
				false, 0, maxSize, "", "", Optional.empty(), "",
				List.of(), List.of(), List.of(), IslandSnapshotS2C.EntityCounts.EMPTY
		);
	}

	private static String resolveName(MinecraftServer server, UUID playerUuid) {
		return server.getUserCache().getByUuid(playerUuid).map(GameProfile::getName).orElse(playerUuid.toString());
	}
}
