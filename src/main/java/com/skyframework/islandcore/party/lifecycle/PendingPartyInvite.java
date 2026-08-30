package com.skyframework.islandcore.party.lifecycle;

import java.time.Instant;
import java.util.UUID;

// Mirrors island.lifecycle.PendingInvite, but for a player being invited to join a party.
public record PendingPartyInvite(UUID partyId, UUID invitedByUuid, UUID inviteeUuid, Instant expiresAt) {
}
