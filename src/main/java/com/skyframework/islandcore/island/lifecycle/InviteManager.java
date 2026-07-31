package com.skyframework.islandcore.island.lifecycle;

import com.skyframework.islandcore.api.island.Island;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InviteManager {

	void invite(UUID islandId, UUID invitedByUuid, UUID inviteeUuid);

	// Consumes and returns the island of the pending, non-expired invite for inviteeUuid,
	// adding them as a MEMBER in the process. Empty if there was none (or it expired).
	Optional<Island> acceptInvite(UUID inviteeUuid);

	// Read-only lookup, doesn't consume the invite (unlike acceptInvite). Empty if there is none
	// pending for invitedUuid, or it already expired. Used by IslandSnapshotBuilder to populate
	// the incoming-invite banner on the client without needing /island accept first.
	Optional<PendingInvite> getPendingInvite(UUID invitedUuid);

	// All non-expired invites this island has sent out and are still awaiting a response, for
	// the owner's own snapshot (IslandSnapshotS2C#pendingInvites). Doesn't consume them.
	List<PendingInvite> getPendingInvitesForIsland(UUID islandId);
}
