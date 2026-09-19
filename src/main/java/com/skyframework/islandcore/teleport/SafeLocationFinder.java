package com.skyframework.islandcore.teleport;

import com.skyframework.islandcore.island.model.IslandBounds;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

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

	// Random-sample + per-column heightmap search — same idiom SafeRandomTeleportFinder already uses
	// for /island rtp — instead of findNearestSafe's exhaustive fixed-Y ring walk above. Built for
	// dimensions where solid ground isn't at one consistent Y (END_LIKE's floating islands are the
	// concrete case: the ring walk only ever checks the ORIGIN column's own Y, so a differently
	// elevated island is invisible to it no matter how large maxRadius is — confirmed by reading
	// TheEndBiomeSource, whose "center" biome alone already spans out to 1024 blocks from origin,
	// with the actual terrain inside that far from a single flat plane). Bounded attempt count keeps
	// this cheap regardless of maxRadius, unlike the ring walk's O(radius²) cost — searches 3
	// progressively wider bands (maxRadius/4, /2, full) so a closer hit is still preferred over a
	// farther one when both exist, without paying the ring walk's per-block price to get that.
	public static Optional<BlockPos> findNearestSafeHeightAware(ServerWorld world, BlockPos target, int maxRadius) {
		if (SafeLandingChecker.isSafe(world, target)) {
			return Optional.of(target);
		}

		Random random = new Random();
		int[] bandRadii = {Math.max(1, maxRadius / 4), Math.max(1, maxRadius / 2), Math.max(1, maxRadius)};
		int attemptsPerBand = 15;

		for (int bandRadius : bandRadii) {
			for (int attempt = 0; attempt < attemptsPerBand; attempt++) {
				double angle = random.nextDouble() * Math.PI * 2;
				int radius = 1 + random.nextInt(bandRadius);
				int x = target.getX() + (int) Math.round(Math.cos(angle) * radius);
				int z = target.getZ() + (int) Math.round(Math.sin(angle) * radius);

				// Forces the chunk to actually load so the block states/heightmap read below reflect
				// real terrain, not an empty/ungenerated chunk — same defensive call
				// SafeRandomTeleportFinder makes before checking.
				world.getChunk(x >> 4, z >> 4);

				int topY = resolveTopY(world, x, z);
				BlockPos candidate = new BlockPos(x, topY, z);
				if (SafeLandingChecker.isSafe(world, candidate)) {
					return Optional.of(candidate);
				}
			}
		}

		return Optional.empty();
	}

	// Vanilla's heightmap (any Heightmap.Type) always scans from the world's own top boundary
	// downward and returns the first column-wide match — for a dimension with a physical ceiling
	// (DimensionType#hasCeiling(), e.g. NETHER_LIKE) that's always the ceiling's own upper surface,
	// never the playable interior below it (confirmed with real captured data during the "aterrizaje
	// seguro" investigation: topY landed exactly on the bedrock ceiling's top face in every tested
	// column). Checked for a reusable vanilla equivalent first — there isn't one: real Nether
	// teleports (portal linking) only search for existing portal frames, not generic safe ground, and
	// every heightmap-based vanilla command (e.g. /spreadplayers) shares this same top-down
	// limitation. So for ceiling dimensions this scans manually, bounded to the dimension's own
	// logicalHeight (128 for real Nether) so it never starts probing the technical air space above the
	// ceiling: first skips downward past whatever's solid right at the top (the ceiling itself), then
	// looks for the next solid surface below that — the same "topmost solid, plus one air space above
	// it" result a heightmap would give, just computed under the ceiling instead of above it. Reuses
	// MOTION_BLOCKING_NO_LEAVES's own block predicate so "solid" means the same thing either way.
	public static int resolveTopY(ServerWorld world, int x, int z) {
		if (!world.getDimension().hasCeiling()) {
			return world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
		}

		Predicate<BlockState> blocking = Heightmap.Type.MOTION_BLOCKING_NO_LEAVES.getBlockPredicate();
		int scanBottom = world.getBottomY();
		int scanTop = scanBottom + world.getDimension().logicalHeight() - 1;
		BlockPos.Mutable pos = new BlockPos.Mutable(x, scanTop, z);

		int y = scanTop;
		boolean solid = blocking.test(world.getBlockState(pos));
		while (y > scanBottom && solid) {
			y--;
			solid = blocking.test(world.getBlockState(pos.setY(y)));
		}
		while (y > scanBottom && !solid) {
			y--;
			solid = blocking.test(world.getBlockState(pos.setY(y)));
		}
		return solid ? y + 1 : scanBottom;
	}
}
