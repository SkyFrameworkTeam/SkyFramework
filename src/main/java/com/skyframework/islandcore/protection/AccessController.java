package com.skyframework.islandcore.protection;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public interface AccessController {

	boolean canBreak(UUID playerUuid, ServerWorld world, BlockPos pos);

	boolean canPlace(UUID playerUuid, ServerWorld world, BlockPos pos);

	boolean canInteractBlock(UUID playerUuid, ServerWorld world, BlockPos pos);

	boolean canOpenContainer(UUID playerUuid, ServerWorld world, BlockPos pos);

	boolean canInteractEntity(UUID playerUuid, ServerWorld world, Entity entity);

	boolean canAttackEntity(UUID playerUuid, ServerWorld world, Entity entity);
}
