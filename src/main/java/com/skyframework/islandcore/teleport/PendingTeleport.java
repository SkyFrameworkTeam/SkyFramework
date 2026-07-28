package com.skyframework.islandcore.teleport;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

// Internal bookkeeping for a teleport currently counting down its warmup; not part of the public api/.
class PendingTeleport {

	final UUID playerUuid;
	final RegistryKey<World> targetDimension;
	final BlockPos targetPos;
	final Vec3d startPosition;
	int ticksRemaining;

	PendingTeleport(UUID playerUuid, RegistryKey<World> targetDimension, BlockPos targetPos, Vec3d startPosition, int ticksRemaining) {
		this.playerUuid = playerUuid;
		this.targetDimension = targetDimension;
		this.targetPos = targetPos;
		this.startPosition = startPosition;
		this.ticksRemaining = ticksRemaining;
	}
}
