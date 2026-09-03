package com.skyframework.islandcore.protection;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.protection.flag.FlagRegistry;
import com.skyframework.islandcore.protection.flag.FlagResolver;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

// Called from mixin/ExplosionMixin. Kept as a plain class (rather than logic inline in the
// mixin) so the actual protection rule stays readable and separate from injected bytecode.
public final class ExplosionProtectionListener {

	private static final RegistryKey<World> ISLANDS_DIMENSION =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

	private ExplosionProtectionListener() {
	}

	// Terrain destruction only: entity damage from the same explosion is untouched (future sprint).
	// pos is the explosion's own origin (Explosion#getPosition, floored) — one flag decision per
	// explosion, not per affected block, since a single blast only has one origin and deciding per
	// block would let an explosion straddling an island's edge apply only partially, which reads as
	// a bug more than a feature. Outside a claimed island — including anywhere outside
	// islandcore:islands, though this is only ever invoked from there — is always vanilla, nothing
	// to protect.
	public static boolean isTerrainDamageBlocked(World world, BlockPos pos) {
		if (!world.getRegistryKey().equals(ISLANDS_DIMENSION)) {
			return false;
		}

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(pos);
		if (maybeIsland.isEmpty()) {
			return false;
		}

		return !FlagResolver.resolveGlobal(maybeIsland.get(), FlagRegistry.EXPLOSION_DAMAGE);
	}
}
