package com.skyframework.islandcore.protection;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.island.IslandPermission;
import com.skyframework.islandcore.island.model.IslandSetting;

import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public class AccessControllerImpl implements AccessController {

	private static final RegistryKey<World> ISLANDS_DIMENSION =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

	@Override
	public boolean canBreak(UUID playerUuid, ServerWorld world, BlockPos pos) {
		return check(playerUuid, world, pos, IslandPermission.BREAK);
	}

	@Override
	public boolean canPlace(UUID playerUuid, ServerWorld world, BlockPos pos) {
		return check(playerUuid, world, pos, IslandPermission.BUILD);
	}

	@Override
	public boolean canInteractBlock(UUID playerUuid, ServerWorld world, BlockPos pos) {
		return check(playerUuid, world, pos, IslandPermission.INTERACT);
	}

	@Override
	public boolean canOpenContainer(UUID playerUuid, ServerWorld world, BlockPos pos) {
		return check(playerUuid, world, pos, IslandPermission.CONTAINERS);
	}

	@Override
	public boolean canInteractEntity(UUID playerUuid, ServerWorld world, Entity entity) {
		return check(playerUuid, world, entity.getBlockPos(), IslandPermission.ENTITIES);
	}

	@Override
	public boolean canAttackEntity(UUID playerUuid, ServerWorld world, Entity entity) {
		return check(playerUuid, world, entity.getBlockPos(), IslandPermission.ENTITIES);
	}

	private boolean check(UUID playerUuid, ServerWorld world, BlockPos pos, IslandPermission permission) {
		if (!world.getRegistryKey().equals(ISLANDS_DIMENSION)) {
			return true;
		}

		// Inside the islands dimension, a position claimed by no island is denied by default
		// (nothing to protect there is not the same as "anyone may do anything").
		Island island = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(pos).orElse(null);
		if (island == null) {
			return false;
		}

		// getIslandAt resolves via the plot (reserved parcel), which can be larger than the
		// island's current physical size. Outside the physical bounds is unbuilt reserve for a
		// future upgrade, not yet part of the island — denied even for the owner.
		if (!island.getBounds().contains(pos)) {
			return false;
		}

		// BUILD_PROTECTION only ever relaxes BUILD/BREAK — INTERACT/CONTAINERS/ENTITIES keep
		// following the role check unconditionally, protection setting or not. A TRUSTED/OWNER
		// member can already build regardless of this setting via hasPermission's own role table
		// below, so this only changes the outcome for players who'd otherwise be denied.
		boolean isBuildOrBreak = permission == IslandPermission.BUILD || permission == IslandPermission.BREAK;
		if (isBuildOrBreak && !island.getSetting(IslandSetting.BUILD_PROTECTION)) {
			return true;
		}

		return island.hasPermission(playerUuid, permission);
	}
}
