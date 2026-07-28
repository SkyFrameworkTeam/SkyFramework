package com.skyframework.islandcore.island.lifecycle;

import java.util.UUID;

public interface IslandDeletionService {

	void requestDeletion(UUID islandId, UUID requestedBy);

	boolean confirmDeletion(UUID islandId, UUID requestedBy);

	// Also called directly (bypassing request/confirm) by IslandRegistryImpl.initializeStorage()
	// to resume any deletion that was interrupted by a server restart. Must be idempotent.
	void executeDeletion(UUID islandId);

	// Called once per server tick by IslandCoreMod's ServerTickEvents.END_SERVER_TICK listener;
	// drives the batched block-clearing step of any deletion(s) in progress.
	void tickAll();
}
