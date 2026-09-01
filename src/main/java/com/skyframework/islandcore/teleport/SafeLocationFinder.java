package com.skyframework.islandcore.teleport;

import com.skyframework.islandcore.island.model.IslandBounds;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

// Finds the nearest position to a target that's safe to land a player on (SafeLandingChecker's
// criteria — the exact same one RTP uses), searching outward in a square spiral at the target's own
// Y level. Deliberately a fixed-Y, 2D search rather than SafeRandomTeleportFinder's
// random-sample-plus-heightmap approach: this is for recovering a SPECIFIC known-good point that's
// had one or a few blocks broken out from under it (a broken home/spawn block), not for finding an
// arbitrary landing spot in open, uneven terrain — the target's own Y is already the right height,
// there's just possibly a missing block or two nearby.
public final class SafeLocationFinder {

	// Shared by every caller (requestHome/requestSpawn/void rescue) so they all search the same
	// radius instead of picking their own magic number — adjust here to change it everywhere at once.
	public static final int DEFAULT_SEARCH_RADIUS = 16;

	private SafeLocationFinder() {
	}

	public static Optional<BlockPos> findNearestSafe(ServerWorld world, BlockPos target, int maxSearchRadius) {
		return findNearestSafe(world, target, maxSearchRadius, null);
	}

	// bounds, if given, is a preference, not a requirement: every ring is checked for an in-bounds
	// match first, and only if the ENTIRE ring has none does it fall back to any match in that ring
	// regardless of bounds — closer-but-outside-the-island beats farther-but-inside it. Callers are
	// expected to fall back to the island's own center (unchecked, a true last resort) if this
	// returns empty — SafeLocationFinder only knows about IslandBounds, not the full Island, so it
	// can't compute that itself.
	public static Optional<BlockPos> findNearestSafe(ServerWorld world, BlockPos target, int maxSearchRadius, IslandBounds bounds) {
		if (SafeLandingChecker.isSafe(world, target)) {
			return Optional.of(target);
		}

		int y = target.getY();
		for (int radius = 1; radius <= maxSearchRadius; radius++) {
			Optional<BlockPos> inBounds = searchRing(world, target, y, radius, bounds);
			if (inBounds.isPresent()) {
				return inBounds;
			}
			if (bounds != null) {
				Optional<BlockPos> anywhere = searchRing(world, target, y, radius, null);
				if (anywhere.isPresent()) {
					return anywhere;
				}
			}
		}

		return Optional.empty();
	}

	// Walks the perimeter of the square ring at the given radius around (target.x, target.z), all
	// at a fixed Y. boundsFilter == null skips the bounds check entirely.
	private static Optional<BlockPos> searchRing(ServerWorld world, BlockPos target, int y, int radius, IslandBounds boundsFilter) {
		int minX = target.getX() - radius;
		int maxX = target.getX() + radius;
		int minZ = target.getZ() - radius;
		int maxZ = target.getZ() + radius;

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				// Only the ring's outline, not the whole filled square — smaller radii already
				// covered the interior on an earlier iteration.
				if (x != minX && x != maxX && z != minZ && z != maxZ) {
					continue;
				}

				BlockPos candidate = new BlockPos(x, y, z);
				if (boundsFilter != null && !boundsFilter.contains(candidate)) {
					continue;
				}

				// Forces the chunk to actually load so the block states read below reflect real
				// terrain — same defensive call SafeRandomTeleportFinder makes before checking.
				world.getChunk(x >> 4, z >> 4);

				if (SafeLandingChecker.isSafe(world, candidate)) {
					return Optional.of(candidate);
				}
			}
		}

		return Optional.empty();
	}
}
