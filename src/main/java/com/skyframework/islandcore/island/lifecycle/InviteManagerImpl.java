package com.skyframework.islandcore.island.lifecycle;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.island.IslandPermission;
import com.skyframework.islandcore.api.network.ActionOutcome;
import com.skyframework.islandcore.api.network.ActionReason;
import com.skyframework.islandcore.island.model.IslandMember;
import com.skyframework.islandcore.island.model.IslandRole;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InviteManagerImpl implements InviteManager {

	private static final Duration INVITE_TIMEOUT = Duration.ofMinutes(5);

	// Lazily cleaned: an expired entry is simply overwritten/ignored on the next invite/accept
	// for that invitee, no dedicated tick loop needed for this.
	private final Map<UUID, PendingInvite> pendingInvites = new HashMap<>();

	@Override
	public ActionOutcome<Void> invite(UUID islandId, UUID invitedByUuid, UUID inviteeUuid) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIsland(islandId);
		if (maybeIsland.isEmpty()) {
			// Callers always resolve islandId from the inviter's own island right before this
			// call, so this shouldn't normally happen; treated the same as "no island" defensively.
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}
		Island island = maybeIsland.get();

		if (island.getOwnerUuid().equals(inviteeUuid)) {
			return ActionOutcome.fail(ActionReason.ALREADY_OWNER);
		}

		IslandRole existingRole = island.getRoleOf(inviteeUuid);
		if (existingRole == IslandRole.MEMBER || existingRole == IslandRole.CO_OWNER) {
			return ActionOutcome.fail(ActionReason.ALREADY_MEMBER);
		}

		pendingInvites.put(inviteeUuid,
				new PendingInvite(islandId, invitedByUuid, inviteeUuid, Instant.now().plus(INVITE_TIMEOUT)));
		return ActionOutcome.ok();
	}

	@Override
	public Optional<Island> acceptInvite(UUID inviteeUuid) {
		PendingInvite invite = pendingInvites.get(inviteeUuid);
		if (invite == null) {
			return Optional.empty();
		}

		if (Instant.now().isAfter(invite.expiresAt())) {
			pendingInvites.remove(inviteeUuid);
			return Optional.empty();
		}

		pendingInvites.remove(inviteeUuid);

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIsland(invite.islandId());
		if (maybeIsland.isEmpty()) {
			// The island got deleted while the invite was pending.
			return Optional.empty();
		}

		Island island = maybeIsland.get();
		IslandMember member = new IslandMember(inviteeUuid, IslandRole.MEMBER, Instant.now(), EnumSet.noneOf(IslandPermission.class));
		IslandCoreMod.ISLAND_REGISTRY.addMember(invite.islandId(), member);

		return Optional.of(island);
	}

	@Override
	public Optional<PendingInvite> getPendingInvite(UUID invitedUuid) {
		PendingInvite invite = pendingInvites.get(invitedUuid);
		if (invite == null || Instant.now().isAfter(invite.expiresAt())) {
			return Optional.empty();
		}
		return Optional.of(invite);
	}

	@Override
	public List<PendingInvite> getPendingInvitesForIsland(UUID islandId) {
		Instant now = Instant.now();
		return pendingInvites.values().stream()
				.filter(invite -> invite.islandId().equals(islandId) && now.isBefore(invite.expiresAt()))
				.toList();
	}
}
