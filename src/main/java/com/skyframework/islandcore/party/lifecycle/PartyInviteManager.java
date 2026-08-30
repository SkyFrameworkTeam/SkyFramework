package com.skyframework.islandcore.party.lifecycle;

import com.skyframework.islandcore.party.model.PartyData;

import java.util.Optional;
import java.util.UUID;

// Mirrors island.lifecycle.InviteManager, but for party membership instead of island membership.
public interface PartyInviteManager {

	// Throws IllegalStateException if inviteeUuid is already in a party (mirrors
	// PartyRegistry#addMember/#createParty's own guard, checked early here so the inviting leader
	// gets immediate feedback instead of only at accept time).
	void requestInvite(UUID partyId, UUID invitedByUuid, UUID inviteeUuid);

	// Consumes and returns the party of the pending, non-expired invite for inviteeUuid, adding
	// them as a member in the process via PartyRegistry#addMember. Empty if there was none (or it
	// expired), or if the invited party no longer exists.
	Optional<PartyData> acceptInvite(UUID inviteeUuid);

	// Read-only lookup, doesn't consume the invite (unlike acceptInvite). Empty if there is none
	// pending for invitedUuid, or it already expired.
	Optional<PendingPartyInvite> getPendingInvite(UUID invitedUuid);
}
