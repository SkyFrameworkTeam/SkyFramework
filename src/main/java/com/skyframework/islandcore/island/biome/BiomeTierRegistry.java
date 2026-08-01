package com.skyframework.islandcore.island.biome;

import com.skyframework.islandcore.api.permission.PermissionProvider;

import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BiomeTierRegistry {

	// A biome not present in any configured tier is never usable, regardless of permissions.
	// A biome present in a tier with permission == null is usable by anyone. Otherwise usable if
	// the player has the permission of at least one of the tiers containing that biome.
	boolean canUse(UUID playerUuid, Identifier biomeId, PermissionProvider permissionProvider);

	// Tiers that contain the given biome, in configured order. Used to build denial messages
	// (e.g. "these tiers would unlock this biome"). Empty if the biome isn't in any tier.
	List<BiomeTier> getTiersContaining(Identifier biomeId);

	// Union of every biome across all tiers this player currently qualifies for (base tiers plus
	// any tier whose permission they hold). Command-autocomplete convenience only: canUse() is
	// still the real gate at execution time, so this must never be trusted as a security check.
	Collection<Identifier> getAvailableBiomes(UUID playerUuid, PermissionProvider permissionProvider);

	// Every configured tier, in configured order, regardless of whether the caller can use them.
	// Added for BiomeTiersS2C (Sprint "acciones de isla"): that packet needs to show every tier
	// (including locked ones) so the client can render "unlocks with tier X" — unlike
	// getAvailableBiomes(), which only returns biomes the player already qualifies for.
	List<BiomeTier> getAllTiers();
}
