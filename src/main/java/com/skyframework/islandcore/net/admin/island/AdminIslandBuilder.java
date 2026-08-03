package com.skyframework.islandcore.net.admin.island;

import com.mojang.authlib.GameProfile;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.island.entity.EntityCategory;
import com.skyframework.islandcore.island.model.IslandMember;
import com.skyframework.islandcore.island.model.IslandRole;
import com.skyframework.islandcore.net.island.IslandSnapshotS2C;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.biome.Biome;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Pure assembly for the Admin Island Manager, mirroring IslandSnapshotBuilder: every field comes
// from an existing service (IslandRegistry via Island, PermissionProvider, IslandEntityTracker),
// no business logic lives here beyond pagination/filter slicing and the live biome lookup.
public final class AdminIslandBuilder {

	private static final DateTimeFormatter DATE_FORMATTER =
			DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

	private AdminIslandBuilder() {
	}

	public static AdminIslandListS2C buildList(MinecraftServer server, Collection<Island> allIslands,
			int requestedPage, int requestedPageSize, String searchQuery) {
		int pageSize = Math.max(1, requestedPageSize);
		String needle = searchQuery == null ? "" : searchQuery.trim().toLowerCase();
		boolean hasSearch = !needle.isEmpty();

		// With no active search, the Spawn island (if it exists) is pulled out of the paginated
		// set entirely — it never counts toward totalPages or any other page's islands — and
		// re-added as an extra row ONLY on the resulting page 0, below. With an active search it's
		// left in `filtered` like any other island: if "Spawn"/its owner name don't match the
		// query, it's just filtered out like everything else, never forced to the top.
		Island spawnIsland = null;
		List<Island> filtered = new ArrayList<>();
		for (Island island : allIslands) {
			boolean isSpawn = island.getOwnerUuid().equals(Island.SERVER_OWNER_UUID);
			if (isSpawn && !hasSearch) {
				spawnIsland = island;
				continue;
			}
			if (!hasSearch || resolveName(server, island.getOwnerUuid()).toLowerCase().contains(needle)) {
				filtered.add(island);
			}
		}

		int totalPages = filtered.isEmpty() ? 0 : (filtered.size() + pageSize - 1) / pageSize;
		int currentPage = totalPages == 0 ? 0 : Math.max(0, Math.min(requestedPage, totalPages - 1));

		List<AdminIslandListS2C.IslandEntry> entries = new ArrayList<>();
		if (spawnIsland != null && currentPage == 0) {
			entries.add(buildEntry(server, spawnIsland));
		}
		if (totalPages > 0) {
			int fromIndex = currentPage * pageSize;
			int toIndex = Math.min(fromIndex + pageSize, filtered.size());
			for (Island island : filtered.subList(fromIndex, toIndex)) {
				entries.add(buildEntry(server, island));
			}
		}

		return new AdminIslandListS2C(entries, totalPages, currentPage);
	}

	private static AdminIslandListS2C.IslandEntry buildEntry(MinecraftServer server, Island island) {
		int maxSize = IslandCoreMod.PERMISSION_PROVIDER.getHighestSizeAllowed(island.getOwnerUuid());
		int memberCount = countMembers(island);

		return new AdminIslandListS2C.IslandEntry(
				island.getOwnerUuid(),
				resolveName(server, island.getOwnerUuid()),
				island.getIslandSize(),
				maxSize,
				island.getIslandType().getId(),
				resolveLiveBiomeId(server, island),
				island.getState().name(),
				memberCount,
				island.getOwnerUuid().equals(Island.SERVER_OWNER_UUID)
		);
	}

	public static AdminIslandDetailS2C buildDetail(MinecraftServer server, Island island) {
		int maxSize = IslandCoreMod.PERMISSION_PROVIDER.getHighestSizeAllowed(island.getOwnerUuid());

		List<IslandSnapshotS2C.MemberEntry> members = new ArrayList<>();
		members.add(new IslandSnapshotS2C.MemberEntry(
				island.getOwnerUuid(), resolveName(server, island.getOwnerUuid()), IslandRole.OWNER.name()));
		for (IslandMember member : island.getMembers()) {
			if (member.role() != IslandRole.MEMBER && member.role() != IslandRole.TRUSTED) {
				continue;
			}
			members.add(new IslandSnapshotS2C.MemberEntry(
					member.playerUuid(), resolveName(server, member.playerUuid()), member.role().name()));
		}

		Map<EntityCategory, Integer> counts = IslandCoreMod.ENTITY_TRACKER.countByCategory(island.getIslandId());
		IslandSnapshotS2C.EntityCounts entities = new IslandSnapshotS2C.EntityCounts(
				counts.getOrDefault(EntityCategory.PLAYERS, 0),
				counts.getOrDefault(EntityCategory.HOSTILE, 0),
				counts.getOrDefault(EntityCategory.PASSIVE, 0),
				counts.getOrDefault(EntityCategory.COBBLEMON, 0),
				counts.getOrDefault(EntityCategory.ITEMS, 0),
				counts.getOrDefault(EntityCategory.OTHER, 0)
		);

		return new AdminIslandDetailS2C(
				island.getIslandId(),
				island.getOwnerUuid(),
				resolveName(server, island.getOwnerUuid()),
				island.getDimension().getValue().toString(),
				island.getGridX(),
				island.getGridZ(),
				island.getCenter(),
				island.getBounds().min(),
				island.getBounds().max(),
				island.getPlotBounds().min(),
				island.getPlotBounds().max(),
				island.getIslandSize(),
				maxSize,
				island.getPlotSize(),
				island.getIslandType().getId(),
				island.getHomeLocation(),
				members,
				island.getState().name(),
				DATE_FORMATTER.format(island.getCreatedAt()),
				DATE_FORMATTER.format(island.getUpdatedAt()),
				entities
		);
	}

	private static int countMembers(Island island) {
		int count = 0;
		for (IslandMember member : island.getMembers()) {
			if (member.role() == IslandRole.MEMBER || member.role() == IslandRole.TRUSTED) {
				count++;
			}
		}
		return count;
	}

	// Deliberately NOT island.getCurrentBiomeId() (our own persisted tracking field, only updated
	// when a player uses /island biome) — the admin list wants what the world's terrain actually
	// is right now, which can drift from that tracking field (e.g. a vanilla /setbiome run directly
	// against the island). Falls back to the tracked field if the island's dimension isn't
	// currently loaded, since there's no live world to query in that case.
	private static String resolveLiveBiomeId(MinecraftServer server, Island island) {
		ServerWorld world = server.getWorld(island.getDimension());
		if (world == null) {
			return island.getCurrentBiomeId();
		}

		RegistryEntry<Biome> biomeEntry = world.getBiome(island.getCenter());
		return biomeEntry.getKey().map(key -> key.getValue().toString()).orElse(island.getCurrentBiomeId());
	}

	private static String resolveName(MinecraftServer server, UUID playerUuid) {
		return server.getUserCache().getByUuid(playerUuid).map(GameProfile::getName).orElse(playerUuid.toString());
	}
}
