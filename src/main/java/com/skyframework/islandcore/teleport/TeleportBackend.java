package com.skyframework.islandcore.teleport;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

// Abstraction over how a teleport is actually performed, so a future backend
// (e.g. Warpstone) can be swapped in without touching TeleportManager.
public interface TeleportBackend {

	boolean teleport(ServerPlayerEntity player, ServerWorld world, BlockPos pos);
}
