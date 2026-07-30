package com.skyframework.islandcore.rtp;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.Optional;
import java.util.Random;
import java.util.Set;

// Search ring is centered on the world origin (0,0), not the world's configured spawn point:
// keeps the radius math simple (matches how "radius" is usually described for /rtp already), and
// our custom dimensions (e.g. islandcore:islands) don't have a single meaningful "world spawn" to
// center around anyway.
public class SafeRandomTeleportFinder {

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

	private final Random random = new Random();

	public Optional<BlockPos> findSafeLocation(ServerWorld world, RtpConfig config) {
		int minRadius = config.getRadiusMin();
		int maxRadius = Math.max(minRadius + 1, config.getRadiusMax());

		for (int attempt = 0; attempt < config.getMaxAttempts(); attempt++) {
			int[] xz = randomColumn(minRadius, maxRadius);
			int x = xz[0];
			int z = xz[1];

			// Forces the chunk to actually load/generate so the block states read below reflect
			// real terrain, not an empty/ungenerated chunk. Accepted as a one-off synchronous cost.
			world.getChunk(x >> 4, z >> 4);

			int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos ground = new BlockPos(x, topY - 1, z);
			BlockPos feet = new BlockPos(x, topY, z);
			BlockPos head = feet.up();

			BlockState groundState = world.getBlockState(ground);
			if (UNSAFE_BLOCKS.contains(groundState.getBlock()) || !groundState.isSolidBlock(world, ground)) {
				continue;
			}

			if (!isFreeAndSafe(world, feet) || !isFreeAndSafe(world, head)) {
				continue;
			}

			return Optional.of(feet);
		}

		return Optional.empty();
	}

	private boolean isFreeAndSafe(ServerWorld world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (UNSAFE_BLOCKS.contains(state.getBlock())) {
			return false;
		}
		return state.isAir() || state.getCollisionShape(world, pos).isEmpty();
	}

	private int[] randomColumn(int minRadius, int maxRadius) {
		double angle = random.nextDouble() * Math.PI * 2;
		int radius = minRadius + random.nextInt(maxRadius - minRadius);

		int x = (int) Math.round(Math.cos(angle) * radius);
		int z = (int) Math.round(Math.sin(angle) * radius);

		return new int[] {x, z};
	}
}
