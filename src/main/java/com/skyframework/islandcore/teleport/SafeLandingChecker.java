package com.skyframework.islandcore.teleport;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

// Shared "is this position safe to land a player on" criteria, extracted so
// SafeRandomTeleportFinder (rtp) and any other teleport destination (sethome, void rescue,
// /island home) all agree on exactly the same definition instead of duplicating it.
public final class SafeLandingChecker {

	// Solid-looking or seemingly-empty blocks that are still dangerous to land on/in.
	private static final Set<Block> UNSAFE_BLOCKS = Set.of(
			Blocks.LAVA,
			Blocks.FIRE,
			Blocks.SOUL_FIRE,
			Blocks.CACTUS,
			Blocks.MAGMA_BLOCK,
			Blocks.WATER,
			Blocks.POWDER_SNOW
	);

	private SafeLandingChecker() {
	}

	// feet is the position the player's feet would occupy after teleporting.
	public static boolean isSafe(ServerWorld world, BlockPos feet) {
		BlockPos ground = feet.down();
		BlockState groundState = world.getBlockState(ground);
		if (UNSAFE_BLOCKS.contains(groundState.getBlock()) || !groundState.isSolidBlock(world, ground)) {
			return false;
		}

		return isFreeAndSafe(world, feet) && isFreeAndSafe(world, feet.up());
	}

	private static boolean isFreeAndSafe(ServerWorld world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (UNSAFE_BLOCKS.contains(state.getBlock())) {
			return false;
		}
		return state.isAir() || state.getCollisionShape(world, pos).isEmpty();
	}
}
