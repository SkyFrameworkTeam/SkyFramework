package com.skyframework.islandcore.teleport;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

public class VanillaTeleportBackend implements TeleportBackend {

	@Override
	public boolean teleport(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		// Empty flag set: all coordinates are absolute. Keep the player's current
		// yaw/pitch (no forced camera reset).
		return player.teleport(
				world,
				pos.getX() + 0.5,
				pos.getY(),
				pos.getZ() + 0.5,
				Set.of(),
				player.getYaw(),
				player.getPitch()
		);
	}
}
