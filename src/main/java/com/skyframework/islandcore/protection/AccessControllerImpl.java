package com.skyframework.islandcore.protection;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.island.IslandPermission;
import com.skyframework.islandcore.island.model.IslandSetting;
import com.skyframework.islandcore.protection.exception.ExceptionGroup;
import com.skyframework.islandcore.protection.exception.ExceptionGroupCategory;
import com.skyframework.islandcore.protection.exception.ExceptionResolver;
import com.skyframework.islandcore.protection.flag.Flag;
import com.skyframework.islandcore.protection.flag.FlagRegistry;
import com.skyframework.islandcore.protection.flag.FlagResolver;

import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

public class AccessControllerImpl implements AccessController {

	private static final RegistryKey<World> ISLANDS_DIMENSION =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

	@Override
	public boolean canBreak(UUID playerUuid, ServerWorld world, BlockPos pos) {
		return checkBlock(playerUuid, world, pos, IslandPermission.BREAK, FlagRegistry.BREAK, true);
	}

	@Override
	public boolean canPlace(UUID playerUuid, ServerWorld world, BlockPos pos) {
		return checkBlock(playerUuid, world, pos, IslandPermission.BUILD, FlagRegistry.BUILD, false);
	}

	@Override
	public boolean canInteractBlock(UUID playerUuid, ServerWorld world, BlockPos pos) {
		return checkBlock(playerUuid, world, pos, IslandPermission.INTERACT, FlagRegistry.INTERACT, false);
	}

	@Override
	public boolean canOpenContainer(UUID playerUuid, ServerWorld world, BlockPos pos) {
		return checkBlock(playerUuid, world, pos, IslandPermission.CONTAINERS, FlagRegistry.CONTAINERS, false);
	}

	@Override
	public boolean canInteractEntity(UUID playerUuid, ServerWorld world, Entity entity) {
		return checkEntity(playerUuid, world, entity, false);
	}

	@Override
	public boolean canAttackEntity(UUID playerUuid, ServerWorld world, Entity entity) {
		return checkEntity(playerUuid, world, entity, true);
	}

	// useAllowBreak selects which ExceptionGroup field a matching exception decides with:
	// isAllowBreak() for canBreak, isAllowInteract() for canPlace/canInteractBlock/canOpenContainer.
	private boolean checkBlock(UUID playerUuid, ServerWorld world, BlockPos pos, IslandPermission permission, Flag flag, boolean useAllowBreak) {
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

		Identifier blockId = Registries.BLOCK.getId(world.getBlockState(pos).getBlock());
		Optional<ExceptionGroup> exception = ExceptionResolver.resolve(island, blockId, ExceptionGroupCategory.BLOCK);
		if (exception.isPresent()) {
			ExceptionGroup group = exception.get();
			if (!group.isRequireEmptyHand() || isMainHandEmpty(world, playerUuid)) {
				return useAllowBreak ? group.isAllowBreak() : group.isAllowInteract();
			}
			// requireEmptyHand not met: this exception doesn't apply, fall through to the normal chain.
		}

		// BUILD_PROTECTION only ever relaxes BUILD/BREAK — INTERACT/CONTAINERS/ENTITIES keep
		// following the flag check unconditionally, protection setting or not. A TRUSTED/OWNER
		// member can already build regardless of this setting via FlagResolver's own role table
		// below, so this only changes the outcome for players who'd otherwise be denied.
		boolean isBuildOrBreak = permission == IslandPermission.BUILD || permission == IslandPermission.BREAK;
		if (isBuildOrBreak && !island.getSetting(IslandSetting.BUILD_PROTECTION)) {
			return true;
		}

		return FlagResolver.resolveForPlayer(island, playerUuid, flag);
	}

	private boolean checkEntity(UUID playerUuid, ServerWorld world, Entity entity, boolean useAllowBreak) {
		if (!world.getRegistryKey().equals(ISLANDS_DIMENSION)) {
			return true;
		}

		BlockPos pos = entity.getBlockPos();

		Island island = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(pos).orElse(null);
		if (island == null) {
			return false;
		}
		if (!island.getBounds().contains(pos)) {
			return false;
		}

		Identifier entityTypeId = Registries.ENTITY_TYPE.getId(entity.getType());
		Optional<ExceptionGroup> exception = ExceptionResolver.resolve(island, entityTypeId, ExceptionGroupCategory.ENTITY);
		if (exception.isPresent()) {
			ExceptionGroup group = exception.get();
			if (!group.isRequireEmptyHand() || isMainHandEmpty(world, playerUuid)) {
				return useAllowBreak ? group.isAllowBreak() : group.isAllowInteract();
			}
		}

		return FlagResolver.resolveForPlayer(island, playerUuid, FlagRegistry.ENTITIES);
	}

	private static boolean isMainHandEmpty(ServerWorld world, UUID playerUuid) {
		ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerUuid);
		return player == null || player.getMainHandStack().isEmpty();
	}
}
