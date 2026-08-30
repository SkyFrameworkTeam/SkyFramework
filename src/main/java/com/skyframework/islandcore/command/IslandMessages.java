package com.skyframework.islandcore.command;

import com.mojang.authlib.GameProfile;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.island.IslandState;
import com.skyframework.islandcore.island.entity.EntityCategory;
import com.skyframework.islandcore.island.model.IslandBounds;
import com.skyframework.islandcore.island.model.IslandMember;
import com.skyframework.islandcore.island.model.IslandRole;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Shared chat-output formatting, used by both the temporary /ic debug command
// tree and the real /island command tree, to avoid duplicating it in both places.
public final class IslandMessages {

	private static final DateTimeFormatter DATE_FORMATTER =
			DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

	private IslandMessages() {
	}

	public static void sendIslandSummary(ServerCommandSource source, Island island) {
		BlockPos center = island.getCenter();
		source.sendFeedback(() -> Text.literal("- " + island.getIslandId()
				+ " owner=" + island.getOwnerUuid()
				+ " center=(" + center.getX() + ", " + center.getY() + ", " + center.getZ() + ")"
				+ " state=" + island.getState()), false);
	}

	// Player-facing view: readable, no raw UUIDs/bounds, technical fields omitted.
	// Used by /island info and /island list.
	public static void sendIslandSummaryPlayer(ServerCommandSource source, Island island) {
		source.sendFeedback(() -> Text.literal("=== Tu Isla ===").formatted(Formatting.BOLD, Formatting.AQUA), false);

		int currentSize = island.getIslandSize();
		int maxSize = IslandCoreMod.PERMISSION_PROVIDER.getHighestSizeAllowed(island.getOwnerUuid());
		source.sendFeedback(() -> labeled("Tamaño: ", currentSize + " / " + maxSize), false);

		source.sendFeedback(() -> labeled("Tipo: ", island.getIslandType().getDisplayName()), false);

		BlockPos home = island.getHomeLocation();
		source.sendFeedback(() -> labeled("Home: ", home.getX() + ", " + home.getY() + ", " + home.getZ()), false);

		if (island.getState() != IslandState.ACTIVE) {
			source.sendFeedback(() -> Text.literal("Estado: " + island.getState()).formatted(Formatting.RED), false);
		}

		sendMembersSectionPlayer(source, island);
		sendEntitySection(source, island);
	}

	// Full technical view, organized into titled sections. Shows exactly the same fields as
	// before (Sprint 14.5 only restyled this, no data added or removed). Used by
	// /island admin list <player>.
	public static void sendIslandSummaryAdmin(ServerCommandSource source, Island island) {
		MinecraftServer server = source.getServer();
		String ownerName = resolveName(server, island.getOwnerUuid());

		source.sendFeedback(() -> Text.literal("=== Isla de " + ownerName + " (ADMIN) ===").formatted(Formatting.BOLD, Formatting.RED), false);

		sectionTitle(source, "Identificadores", Formatting.GRAY);
		source.sendFeedback(() -> labeled("islandId: ", island.getIslandId().toString()), false);
		source.sendFeedback(() -> labeled("ownerUuid: ", island.getOwnerUuid().toString()), false);

		sectionTitle(source, "Ubicación y Tamaño", Formatting.AQUA);
		source.sendFeedback(() -> labeled("dimension: ", island.getDimension().getValue().toString()), false);
		source.sendFeedback(() -> labeled("grid: ", island.getGridX() + ", " + island.getGridZ()), false);
		BlockPos center = island.getCenter();
		source.sendFeedback(() -> labeled("center: ", center.getX() + ", " + center.getY() + ", " + center.getZ()), false);
		source.sendFeedback(() -> labeled("bounds: ", formatBounds(island.getBounds())), false);
		source.sendFeedback(() -> labeled("plotBounds: ", formatBounds(island.getPlotBounds())), false);
		source.sendFeedback(() -> labeled("islandSize: ", String.valueOf(island.getIslandSize())), false);
		source.sendFeedback(() -> labeled("plotSize: ", String.valueOf(island.getPlotSize())), false);
		source.sendFeedback(() -> labeled("islandType: ", island.getIslandType().getId()), false);
		BlockPos home = island.getHomeLocation();
		source.sendFeedback(() -> labeled("homeLocation: ", home.getX() + ", " + home.getY() + ", " + home.getZ()), false);

		sendMembersSectionAdmin(source, island);

		sectionTitle(source, "Estado y Fechas", Formatting.GRAY);
		Formatting stateColor = island.getState() == IslandState.ACTIVE ? Formatting.WHITE : Formatting.RED;
		source.sendFeedback(() -> Text.literal("state: ").formatted(Formatting.GRAY)
				.append(Text.literal(island.getState().toString()).formatted(stateColor)), false);
		source.sendFeedback(() -> labeled("createdAt: ", DATE_FORMATTER.format(island.getCreatedAt())), false);
		source.sendFeedback(() -> labeled("updatedAt: ", DATE_FORMATTER.format(island.getUpdatedAt())), false);

		sendEntitySection(source, island);
	}

	private static void sendMembersSectionAdmin(ServerCommandSource source, Island island) {
		sectionTitle(source, "Miembros", Formatting.GOLD);

		MinecraftServer server = source.getServer();

		source.sendFeedback(() -> memberLineAdmin(server, island.getOwnerUuid(), IslandRole.OWNER), false);

		for (IslandMember member : island.getMembers()) {
			if (member.role() != IslandRole.MEMBER && member.role() != IslandRole.TRUSTED) {
				continue;
			}
			source.sendFeedback(() -> memberLineAdmin(server, member.playerUuid(), member.role()), false);
		}
	}

	// Package-visible (not private): reused by IslandCommand's /island flags listing for the same
	// titled-section look as sendIslandSummaryPlayer/sendIslandSummaryAdmin.
	static void sectionTitle(ServerCommandSource source, String title, Formatting color) {
		source.sendFeedback(() -> Text.literal(title).formatted(Formatting.BOLD, color), false);
	}

	private static String formatBounds(IslandBounds bounds) {
		BlockPos min = bounds.min();
		BlockPos max = bounds.max();
		return "(" + min.getX() + ", " + min.getY() + ", " + min.getZ() + ") -> ("
				+ max.getX() + ", " + max.getY() + ", " + max.getZ() + ")";
	}

	private static void sendMembersSectionPlayer(ServerCommandSource source, Island island) {
		source.sendFeedback(() -> Text.literal("Miembros").formatted(Formatting.BOLD, Formatting.GOLD), false);

		MinecraftServer server = source.getServer();

		source.sendFeedback(() -> memberLine(server, island.getOwnerUuid(), IslandRole.OWNER), false);

		for (IslandMember member : island.getMembers()) {
			// VISITOR/DENIED aren't explicit members: nothing currently stores them here, but
			// filter defensively in case that ever changes.
			if (member.role() != IslandRole.MEMBER && member.role() != IslandRole.TRUSTED) {
				continue;
			}
			source.sendFeedback(() -> memberLine(server, member.playerUuid(), member.role()), false);
		}
	}

	private static Text memberLine(MinecraftServer server, UUID playerUuid, IslandRole role) {
		String name = resolveName(server, playerUuid);
		return Text.literal("- " + name + " ")
				.append(Text.literal("(" + role + ")").formatted(roleColor(role)));
	}

	private static Text memberLineAdmin(MinecraftServer server, UUID playerUuid, IslandRole role) {
		String name = resolveName(server, playerUuid);
		return Text.literal("- " + name + " (" + playerUuid + ") ")
				.append(Text.literal("[" + role + "]").formatted(roleColor(role)));
	}

	private static Formatting roleColor(IslandRole role) {
		return switch (role) {
			case OWNER -> Formatting.GOLD;
			case MEMBER -> Formatting.GREEN;
			case TRUSTED -> Formatting.AQUA;
			case ALLY -> Formatting.YELLOW;
			case VISITOR, DENIED -> Formatting.GRAY;
		};
	}

	private static String resolveName(MinecraftServer server, UUID playerUuid) {
		return server.getUserCache().getByUuid(playerUuid).map(GameProfile::getName).orElse(playerUuid.toString());
	}

	private static void sendEntitySection(ServerCommandSource source, Island island) {
		source.sendFeedback(() -> Text.literal("Entidades").formatted(Formatting.BOLD, Formatting.LIGHT_PURPLE), false);

		Map<EntityCategory, Integer> counts = IslandCoreMod.ENTITY_TRACKER.countByCategory(island.getIslandId());
		int total = counts.values().stream().mapToInt(Integer::intValue).sum();

		List<String> parts = new ArrayList<>();
		addCategoryPart(parts, counts, EntityCategory.PLAYERS, "jugador", "jugadores");
		addCategoryPart(parts, counts, EntityCategory.PASSIVE, "pasivo", "pasivos");
		addCategoryPart(parts, counts, EntityCategory.HOSTILE, "hostil", "hostiles");
		addCategoryPart(parts, counts, EntityCategory.ITEMS, "objeto", "objetos");
		addCategoryPart(parts, counts, EntityCategory.COBBLEMON, "Cobblemon", "Cobblemon");
		addCategoryPart(parts, counts, EntityCategory.OTHER, "otro", "otros");

		String summary = "Total (" + total + ")" + (parts.isEmpty() ? "" : ": " + String.join(", ", parts));
		source.sendFeedback(() -> Text.literal(summary), false);
	}

	private static void addCategoryPart(
			List<String> parts, Map<EntityCategory, Integer> counts, EntityCategory category, String singular, String plural) {
		int count = counts.getOrDefault(category, 0);
		if (count > 0) {
			parts.add(count + " " + (count == 1 ? singular : plural));
		}
	}

	// Package-visible (not private): reused by IslandCommand's /island flags listing.
	static Text labeled(String label, String value) {
		return Text.literal(label).formatted(Formatting.GRAY).append(Text.literal(value).formatted(Formatting.WHITE));
	}
}
