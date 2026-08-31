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

	// playerUuid is the requester (usually, but not necessarily, island.getOwnerUuid() — an
	// incoming invite belongs to the PLAYER asking for their own snapshot, not to the island being
	// described, so it must be threaded through explicitly rather than assumed from the island).
	public static IslandSnapshotS2C build(MinecraftServer server, UUID playerUuid, Island island) {
		int maxSize = IslandCoreMod.PERMISSION_PROVIDER.getHighestSizeAllowed(island.getOwnerUuid());

		int biomeCooldownRemainingSeconds = 0;
		Instant lastBiomeChange = island.getLastBiomeChangeAt();
		if (lastBiomeChange != null) {
			long cooldownSeconds = IslandCoreMod.PERMISSION_PROVIDER.getBiomeCooldownSeconds(island.getOwnerUuid());
			Instant availableAt = lastBiomeChange.plusSeconds(cooldownSeconds);
			if (Instant.now().isBefore(availableAt)) {
				biomeCooldownRemainingSeconds = (int) Duration.between(Instant.now(), availableAt).getSeconds();
			}
		}

		Optional<IslandSnapshotS2C.IncomingInviteEntry> incomingInvite = IslandCoreMod.INVITE_MANAGER.getPendingInvite(playerUuid)
				.map(invite -> new IslandSnapshotS2C.IncomingInviteEntry(
						resolveName(server, invite.invitedByUuid()),
						(int) Math.max(0L, Duration.between(Instant.now(), invite.expiresAt()).getSeconds())));

		List<IslandSnapshotS2C.MemberEntry> members = new ArrayList<>();
		members.add(new IslandSnapshotS2C.MemberEntry(
				island.getOwnerUuid(), resolveName(server, island.getOwnerUuid()), IslandRole.OWNER.name()));
		for (IslandMember member : island.getMembers()) {
			// MEMBER/TRUSTED/ALLY are real, explicitly-granted entries (mirrors IslandMessages'
			// member sections, extended for ALLY — added alongside the party sprint's
			// "/island ally add"). VISITOR/DENIED aren't shown as members. Note this only ever
			// contains explicit per-player ALLY grants: an ALLY resolved implicitly from a party
			// alliance (see IslandData#getRoleOf) is never stored as an IslandMember entry, so it
			// never appears here either — same as party-derived MEMBER status already didn't before
			// this change.
			if (member.role() != IslandRole.MEMBER && member.role() != IslandRole.TRUSTED && member.role() != IslandRole.ALLY) {
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
				biomeCooldownRemainingSeconds,
				Optional.of(island.getHomeLocation()),
				island.getState().name(),
				members,
				pendingInvites,
				incomingInvite,
				settings,
				entities
		);
	}

	// maxSize still comes from PermissionProvider even without an island yet, so the client can
	// show what a future island's cap would be. An incoming invite is still checked here (unlike
	// every other field, which is empty/default) since this is exactly the scenario
	// InviteManager#getPendingInvite exists for: a player with no island yet who's been invited to
	// someone else's, so they can see and accept it without needing an island of their own first.
	public static IslandSnapshotS2C buildEmpty(MinecraftServer server, UUID playerUuid) {
		int maxSize = IslandCoreMod.PERMISSION_PROVIDER.getHighestSizeAllowed(playerUuid);

		Optional<IslandSnapshotS2C.IncomingInviteEntry> incomingInvite = IslandCoreMod.INVITE_MANAGER.getPendingInvite(playerUuid)
				.map(invite -> new IslandSnapshotS2C.IncomingInviteEntry(
						resolveName(server, invite.invitedByUuid()),
						(int) Math.max(0L, Duration.between(Instant.now(), invite.expiresAt()).getSeconds())));

		return new IslandSnapshotS2C(
				false, 0, maxSize, "", "", 0, Optional.empty(), "",
				List.of(), List.of(), incomingInvite, List.of(), IslandSnapshotS2C.EntityCounts.EMPTY
		);
	}

	private static String resolveName(MinecraftServer server, UUID playerUuid) {
		return server.getUserCache().getByUuid(playerUuid).map(GameProfile::getName).orElse(playerUuid.toString());
	}
}
