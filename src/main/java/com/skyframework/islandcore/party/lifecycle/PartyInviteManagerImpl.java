package com.skyframework.islandcore.party.lifecycle;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.party.model.PartyData;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PartyInviteManagerImpl implements PartyInviteManager {

	private static final Duration INVITE_TIMEOUT = Duration.ofMinutes(5);

	// Lazily cleaned: an expired entry is simply overwritten/ignored on the next invite/accept
	// for that invitee, no dedicated tick loop needed for this — mirrors InviteManagerImpl.
	private final Map<UUID, PendingPartyInvite> pendingInvites = new HashMap<>();

	@Override
	public void requestInvite(UUID partyId, UUID invitedByUuid, UUID inviteeUuid) {
		if (IslandCoreMod.PARTY_REGISTRY.getPartyOf(inviteeUuid).isPresent()) {
			throw new IllegalStateException("Ese jugador ya pertenece a una party.");
		}

		pendingInvites.put(inviteeUuid, new PendingPartyInvite(partyId, invitedByUuid, inviteeUuid, Instant.now().plus(INVITE_TIMEOUT)));
	}

	@Override
	public Optional<PartyData> acceptInvite(UUID inviteeUuid) {
		PendingPartyInvite invite = pendingInvites.get(inviteeUuid);
		if (invite == null) {
			return Optional.empty();
		}

		if (Instant.now().isAfter(invite.expiresAt())) {
			pendingInvites.remove(inviteeUuid);
			return Optional.empty();
		}

		pendingInvites.remove(inviteeUuid);

		Optional<PartyData> maybeParty = IslandCoreMod.PARTY_REGISTRY.getParty(invite.partyId());
		if (maybeParty.isEmpty()) {
			// The party got disbanded while the invite was pending.
			return Optional.empty();
		}

		// Propagates IllegalStateException if the invitee joined a different party in the meantime
		// (e.g. accepted another invite first) — the caller (PartyCommand) reports that the same way
		// it reports every other PartyRegistry validation failure.
		IslandCoreMod.PARTY_REGISTRY.addMember(invite.partyId(), inviteeUuid);

		return maybeParty;
	}

	@Override
	public Optional<PendingPartyInvite> getPendingInvite(UUID invitedUuid) {
		PendingPartyInvite invite = pendingInvites.get(invitedUuid);
		if (invite == null || Instant.now().isAfter(invite.expiresAt())) {
			return Optional.empty();
		}
		return Optional.of(invite);
	}
}
