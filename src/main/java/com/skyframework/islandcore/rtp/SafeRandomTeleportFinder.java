package com.skyframework.islandcore.rtp;

import com.skyframework.islandcore.teleport.SafeLandingChecker;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.Optional;
import java.util.Random;

// Search ring is centered on the world origin (0,0), not the world's configured spawn point:
// keeps the radius math simple (matches how "radius" is usually described for /rtp already), and
// our custom dimensions (e.g. islandcore:islands) don't have a single meaningful "world spawn" to
// center around anyway.
public class SafeRandomTeleportFinder {

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
			BlockPos feet = new BlockPos(x, topY, z);

			if (!SafeLandingChecker.isSafe(world, feet)) {
				continue;
			}

			return Optional.of(feet);
		}

		return Optional.empty();
	}

	private int[] randomColumn(int minRadius, int maxRadius) {
		double angle = random.nextDouble() * Math.PI * 2;
		int radius = minRadius + random.nextInt(maxRadius - minRadius);

		int x = (int) Math.round(Math.cos(angle) * radius);
		int z = (int) Math.round(Math.sin(angle) * radius);

		return new int[] {x, z};
	}
}
