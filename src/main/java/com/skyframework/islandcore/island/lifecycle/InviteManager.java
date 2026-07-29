package com.skyframework.islandcore.island.lifecycle;

import com.skyframework.islandcore.api.island.Island;

import java.util.Optional;
import java.util.UUID;

public interface InviteManager {

	void invite(UUID islandId, UUID invitedByUuid, UUID inviteeUuid);

	// Consumes and returns the island of the pending, non-expired invite for inviteeUuid,
	// adding them as a MEMBER in the process. Empty if there was none (or it expired).
	Optional<Island> acceptInvite(UUID inviteeUuid);
}
