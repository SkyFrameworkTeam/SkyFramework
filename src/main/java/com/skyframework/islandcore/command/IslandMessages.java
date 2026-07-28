package com.skyframework.islandcore.command;

import com.skyframework.islandcore.api.island.Island;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

// Shared chat-output formatting, used by both the temporary /ic debug command
// tree and the real /island command tree, to avoid duplicating it in both places.
public final class IslandMessages {

	private IslandMessages() {
	}

	public static void sendIslandSummary(ServerCommandSource source, Island island) {
		BlockPos center = island.getCenter();
		source.sendFeedback(() -> Text.literal("- " + island.getIslandId()
				+ " owner=" + island.getOwnerUuid()
				+ " center=(" + center.getX() + ", " + center.getY() + ", " + center.getZ() + ")"
				+ " state=" + island.getState()), false);
	}

	public static void sendIslandDetails(ServerCommandSource source, Island island) {
		BlockPos center = island.getCenter();
		BlockPos home = island.getHomeLocation();

		source.sendFeedback(() -> Text.literal("islandId=" + island.getIslandId()), false);
		source.sendFeedback(() -> Text.literal("ownerUuid=" + island.getOwnerUuid()), false);
		source.sendFeedback(() -> Text.literal("dimension=" + island.getDimension().getValue()), false);
		source.sendFeedback(() -> Text.literal("grid=(" + island.getGridX() + ", " + island.getGridZ() + ")"), false);
		source.sendFeedback(() -> Text.literal("center=(" + center.getX() + ", " + center.getY() + ", " + center.getZ() + ")"), false);
		source.sendFeedback(() -> Text.literal("bounds=" + island.getBounds()), false);
		source.sendFeedback(() -> Text.literal("plotBounds=" + island.getPlotBounds()), false);
		source.sendFeedback(() -> Text.literal("islandSize=" + island.getIslandSize()), false);
		source.sendFeedback(() -> Text.literal("plotSize=" + island.getPlotSize()), false);
		source.sendFeedback(() -> Text.literal("islandType=" + island.getIslandType().getId()), false);
		source.sendFeedback(() -> Text.literal("homeLocation=(" + home.getX() + ", " + home.getY() + ", " + home.getZ() + ")"), false);
		source.sendFeedback(() -> Text.literal("state=" + island.getState()), false);
		source.sendFeedback(() -> Text.literal("members=" + island.getMembers()), false);
		source.sendFeedback(() -> Text.literal("createdAt=" + island.getCreatedAt()), false);
		source.sendFeedback(() -> Text.literal("updatedAt=" + island.getUpdatedAt()), false);
	}
}
