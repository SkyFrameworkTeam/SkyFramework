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
import net.minecraft.entity.LivingEntity;
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
		return checkBlock(playerUuid, world, pos, IslandPermission.CONSTRUCCION, FlagRegistry.CONSTRUCCION, true);
	}

	@Override
	public boolean canPlace(UUID playerUuid, ServerWorld world, BlockPos pos) {
		return checkBlock(playerUuid, world, pos, IslandPermission.CONSTRUCCION, FlagRegistry.CONSTRUCCION, false);
	}

	@Override
	public boolean canInteractBlock(UUID playerUuid, ServerWorld world, BlockPos pos) {
		return checkBlock(playerUuid, world, pos, IslandPermission.INTERACT, FlagRegistry.INTERACT, false);
	}

	// No more generic "containers" flag (retired — see FlagRegistry): checkBlock's own
	// exception-group lookup below already resolves per concrete container type (chests, furnaces,
	// barrels, shulker_boxes, hoppers, dispensers_droppers, each its own group with its own
	// patterns) BEFORE ever reaching the flag fallback passed here — a matched group decides the
	// outcome entirely on its own, with no flag involved at all. INTERACT is only reached as a
	// fallback for a container type that matches none of those groups (a modded container, or one
	// nobody's defined a group for yet): the same generic "can this role touch things on this
	// island" flag canInteractBlock already uses for non-container blocks, rather than leaving such
	// a container with no role-based protection at all.
	@Override
	public boolean canOpenContainer(UUID playerUuid, ServerWorld world, BlockPos pos) {
		return checkBlock(playerUuid, world, pos, IslandPermission.INTERACT, FlagRegistry.INTERACT, false);
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
		Optional<ExceptionGroup> exception = ExceptionResolver.resolve(island, playerUuid, blockId, ExceptionGroupCategory.BLOCK);
		if (exception.isPresent()) {
			ExceptionGroup group = exception.get();
			if (!group.isRequireEmptyHand() || isMainHandEmpty(world, playerUuid)) {
				return useAllowBreak ? group.isAllowBreak() : group.isAllowInteract();
			}
			// requireEmptyHand not met: this exception doesn't apply, fall through to the normal chain.
		}

		// BUILD_PROTECTION only ever relaxes CONSTRUCCION (placing/breaking) — INTERACT (including
		// containers, now that "containers" isn't its own flag) and ENTITIES keep following the flag
		// check unconditionally, protection setting or not. A
		// CO_OWNER/OWNER member can already build regardless of this setting via FlagResolver's own
		// role table below, so this only changes the outcome for players who'd otherwise be denied.
		boolean isConstruccion = permission == IslandPermission.CONSTRUCCION;
		if (isConstruccion && !island.getSetting(IslandSetting.BUILD_PROTECTION)) {
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

		// Owner/ENTITIES bypass for the ATTACK path on a LivingEntity, checked BEFORE the
		// exception-group lookup below: an owner/trusted member is meant to be exempt from every
		// restriction on their own island, not just the role-based ones. Without this, an example
		// exception group like "animals" (allowBreak=false for minecraft:horse, meant to let a
		// stranger pet/ride a horse without letting them kill it) would ALSO block the owner from
		// ever attacking their own horse — exception groups have no innate owner-exemption of their
		// own, unlike FlagResolver.resolveForPlayer's OWNER short-circuit. Scoped to
		// useAllowBreak+LivingEntity only (the attack path): canInteractEntity (mounting/petting)
		// is untouched, and non-living attack targets still fall through to the exception check.
		if (useAllowBreak && entity instanceof LivingEntity
				&& FlagResolver.resolveForPlayer(island, playerUuid, FlagRegistry.ENTITIES)) {
			return true;
		}

		Identifier entityTypeId = Registries.ENTITY_TYPE.getId(entity.getType());
		Optional<ExceptionGroup> exception = ExceptionResolver.resolve(island, playerUuid, entityTypeId, ExceptionGroupCategory.ENTITY);
		if (exception.isPresent()) {
			ExceptionGroup group = exception.get();
			if (!group.isRequireEmptyHand() || isMainHandEmpty(world, playerUuid)) {
				return useAllowBreak ? group.isAllowBreak() : group.isAllowInteract();
			}
		}

		// Attacking (useAllowBreak=true) a LivingEntity — a player or a mob — is what triggers
		// ServerLivingEntityEvents.ALLOW_DAMAGE afterward, which DamageProtectionListener already
		// governs with PVP_DAMAGE/MOB_DAMAGE (replicating an ENTITIES bypass ONLY for the
		// MOB_DAMAGE/mixed-side case, deliberately never for PVP — see that class's javadoc).
		// Deferring here instead of also vetoing on ENTITIES avoids this role-based flag silently
		// overriding those island-global toggles for every attacker who isn't a trusted member —
		// previously a VISITOR could never even land a hit regardless of PVP_DAMAGE/MOB_DAMAGE
		// being set to true, since this check ran first and denied outright. Non-living attack
		// targets (item frames, paintings, etc.) never reach ALLOW_DAMAGE, so ENTITIES remains
		// their only protection.
		if (useAllowBreak && entity instanceof LivingEntity) {
			return true;
		}

		// ENTITIES governs attacking ONLY (useAllowBreak=true — combat on a non-living target here,
		// e.g. an item frame or armor stand, since the LivingEntity attack case already returned
		// above). Any non-violent interaction (useAllowBreak=false — mounting, taming, feeding,
		// leashing, and everything else canInteractEntity covers) falls through to INTERACT instead,
		// the same generic flag canInteractBlock already uses for blocks — this used to
		// unconditionally check ENTITIES here regardless of useAllowBreak, which meant a role denied
		// only ENTITIES (combat) also couldn't pet/tame/feed anything, and a role allowed INTERACT
		// but not ENTITIES still couldn't either; the two actions are now independent.
		return FlagResolver.resolveForPlayer(island, playerUuid, useAllowBreak ? FlagRegistry.ENTITIES : FlagRegistry.INTERACT);
	}

	private static boolean isMainHandEmpty(ServerWorld world, UUID playerUuid) {
		ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerUuid);
		return player == null || player.getMainHandStack().isEmpty();
	}
}
