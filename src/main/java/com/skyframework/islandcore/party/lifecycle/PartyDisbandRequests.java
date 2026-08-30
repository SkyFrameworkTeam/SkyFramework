package com.skyframework.islandcore.party.lifecycle;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Shared "/party disband" confirmation state (a lightweight 15s window, per the sprint that
// introduced /party disband — nowhere near IslandDeletionService's full 30s/action-bar mechanism,
// since disbanding a party isn't as destructive as deleting an island). Extracted out of
// PartyCommand so both the text command and the network path (PartyDisbandRequestC2S/
// PartyDisbandConfirmC2S) share one source of truth: a disband requested from one must be
// confirmable from the other.
public final class PartyDisbandRequests {

	public static final Duration TIMEOUT = Duration.ofSeconds(15);

	private static final Map<UUID, Instant> pending = new HashMap<>();

	private PartyDisbandRequests() {
	}

	public static void request(UUID partyId) {
		pending.put(partyId, Instant.now().plus(TIMEOUT));
	}

	// Consumes the pending request if present and not expired; returns whether it was confirmed.
	public static boolean confirm(UUID partyId) {
		Instant expiresAt = pending.remove(partyId);
		return expiresAt != null && Instant.now().isBefore(expiresAt);
	}
}
