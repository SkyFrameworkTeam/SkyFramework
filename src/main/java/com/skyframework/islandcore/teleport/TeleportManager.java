package com.skyframework.islandcore.teleport;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public interface TeleportManager {

	void requestHome(ServerPlayerEntity player);

	void requestSpawn(ServerPlayerEntity player);

	// Called once per server tick by IslandCoreMod's ServerTickEvents.END_SERVER_TICK listener;
	// not meant to be called by other systems.
	void tickAll();

	void cancelPendingTeleport(UUID playerUuid, String reason);
}
