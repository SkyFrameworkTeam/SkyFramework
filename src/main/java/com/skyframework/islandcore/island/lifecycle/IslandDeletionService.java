package com.skyframework.islandcore.island.lifecycle;

import java.util.Optional;
import java.util.UUID;

public interface IslandDeletionService {

	void requestDeletion(UUID islandId, UUID requestedBy);

	boolean confirmDeletion(UUID islandId, UUID requestedBy);

	// Read-only peek, doesn't consume/alter the pending request. Empty if there's no pending
	// deletion for this island (or it already expired). Used by the network layer to report the
	// current countdown (PendingConfirmationTickS2C) without depending on the action-bar text
	// tickPendingRequests() already sends.
	Optional<Long> getPendingDeletionRemainingSeconds(UUID islandId);

	// Also called directly (bypassing request/confirm) by IslandRegistryImpl.initializeStorage()
	// to resume any deletion that was interrupted by a server restart. Must be idempotent.
	void executeDeletion(UUID islandId);

	// Called once per server tick by IslandCoreMod's ServerTickEvents.END_SERVER_TICK listener;
	// drives the batched block-clearing step of any deletion(s) in progress.
	void tickAll();
}
