package com.skyframework.islandcore.island.lifecycle;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.island.IslandPermission;
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
	public void invite(UUID islandId, UUID invitedByUuid, UUID inviteeUuid) {
		Island island = IslandCoreMod.ISLAND_REGISTRY.getIsland(islandId)
				.orElseThrow(() -> new IllegalArgumentException("No existe ninguna isla con id: " + islandId));

		if (island.getOwnerUuid().equals(inviteeUuid)) {
			throw new IllegalArgumentException("Ya eres el propietario de esta isla.");
		}

		IslandRole existingRole = island.getRoleOf(inviteeUuid);
		if (existingRole == IslandRole.MEMBER || existingRole == IslandRole.TRUSTED) {
			throw new IllegalArgumentException("Ese jugador ya es miembro de tu isla.");
		}

		pendingInvites.put(inviteeUuid,
				new PendingInvite(islandId, invitedByUuid, inviteeUuid, Instant.now().plus(INVITE_TIMEOUT)));
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
