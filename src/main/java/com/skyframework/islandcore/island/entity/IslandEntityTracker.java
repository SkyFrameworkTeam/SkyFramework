package com.skyframework.islandcore.island.entity;

import net.minecraft.entity.Entity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IslandEntityTracker {

	// All entities (players included) within the island's full reserved plot (getPlotBounds()),
	// not just the physically built area.
	List<Entity> getEntitiesInIsland(UUID islandId);

	Map<EntityCategory, Integer> countByCategory(UUID islandId);

	int countEntitiesInIsland(UUID islandId);
}
