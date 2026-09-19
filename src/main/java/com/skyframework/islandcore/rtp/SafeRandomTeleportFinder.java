package com.skyframework.islandcore.rtp;

import com.skyframework.islandcore.teleport.SafeLandingChecker;
import com.skyframework.islandcore.teleport.SafeLocationFinder;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

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

			// Shared with resolveSafeLanding's own ground detection (Sprint "aterrizaje seguro"
			// investigation, part 1): the raw MOTION_BLOCKING_NO_LEAVES heightmap this used to call
			// directly finds the bedrock ceiling's own top face in any has_ceiling=true dimension
			// (e.g. NETHER_LIKE), not the playable interior below it — confirmed with real /rtp
			// samples all landing at y=128 in a roofed test dimension. resolveTopY is ceiling-aware.
			int topY = SafeLocationFinder.resolveTopY(world, x, z);
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
