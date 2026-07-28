package com.skyframework.islandcore.protection;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

// Called from mixin/ExplosionMixin. Kept as a plain class (rather than logic inline in the
// mixin) so the actual protection rule stays readable and separate from injected bytecode.
public final class ExplosionProtectionListener {

	private static final RegistryKey<World> ISLANDS_DIMENSION =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

	private ExplosionProtectionListener() {
	}

	// Terrain destruction only: entity damage from the same explosion is untouched (future sprint).
	public static boolean isTerrainDamageBlocked(World world) {
		return world.getRegistryKey().equals(ISLANDS_DIMENSION);
	}
}
