package com.skyframework.islandcore.island.entity;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.island.model.IslandBounds;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class IslandEntityTrackerImpl implements IslandEntityTracker {

	// onInitialize() runs before the MinecraftServer exists, so the reference is captured lazily on server start.
	private MinecraftServer server;

	public IslandEntityTrackerImpl() {
		ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> this.server = startedServer);
	}

	@Override
	public List<Entity> getEntitiesInIsland(UUID islandId) {
		if (server == null) {
			return List.of();
		}

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIsland(islandId);
		if (maybeIsland.isEmpty()) {
			return List.of();
		}

		Island island = maybeIsland.get();
		ServerWorld world = server.getWorld(island.getDimension());
		if (world == null) {
			return List.of();
		}

		IslandBounds bounds = island.getPlotBounds();
		Box box = Box.enclosing(bounds.min(), bounds.max());

		return world.getOtherEntities(null, box, entity -> true);
	}

	@Override
	public Map<EntityCategory, Integer> countByCategory(UUID islandId) {
		Map<EntityCategory, Integer> counts = new EnumMap<>(EntityCategory.class);

		for (Entity entity : getEntitiesInIsland(islandId)) {
			counts.merge(categorize(entity), 1, Integer::sum);
		}

		return counts;
	}

	@Override
	public int countEntitiesInIsland(UUID islandId) {
		return getEntitiesInIsland(islandId).size();
	}

	// Checked out of the enum's declared order on purpose: Cobblemon entities are LivingEntity
	// (often Monster) subclasses, but must still be counted as COBBLEMON rather than
	// HOSTILE/PASSIVE, so that check runs before the Monster/LivingEntity ones.
	private static EntityCategory categorize(Entity entity) {
		if (entity instanceof PlayerEntity) {
			return EntityCategory.PLAYERS;
		}
		if (Registries.ENTITY_TYPE.getId(entity.getType()).getNamespace().equals("cobblemon")) {
			return EntityCategory.COBBLEMON;
		}
		if (entity instanceof Monster) {
			return EntityCategory.HOSTILE;
		}
		if (entity instanceof LivingEntity) {
			return EntityCategory.PASSIVE;
		}
		if (entity instanceof ItemEntity) {
			return EntityCategory.ITEMS;
		}
		return EntityCategory.OTHER;
	}
}
