package com.skyframework.islandcore.net.admin.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One page of the admin island list, built by {@link AdminIslandBuilder}. Wire field order:
 * {@code islands} (list of {@link IslandEntry}), {@code totalPages} (int, 0 if the filtered result
 * is empty), {@code currentPage} (int, the same 0-indexed page the request asked for, clamped into
 * {@code [0, totalPages - 1]} if it was out of range).
 */
public record AdminIslandListS2C(List<IslandEntry> islands, int totalPages, int currentPage) implements CustomPayload {

	public static final CustomPayload.Id<AdminIslandListS2C> ID = new CustomPayload.Id<>(NetworkChannels.ADMIN_ISLAND_LIST_S2C);

	private static final PacketCodec<RegistryByteBuf, List<IslandEntry>> ISLAND_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, IslandEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, AdminIslandListS2C> CODEC = PacketCodec.tuple(
			ISLAND_LIST_CODEC, AdminIslandListS2C::islands,
			PacketCodecs.VAR_INT, AdminIslandListS2C::totalPages,
			PacketCodecs.VAR_INT, AdminIslandListS2C::currentPage,
			AdminIslandListS2C::new
	);

	@Override
	public CustomPayload.Id<AdminIslandListS2C> getId() {
		return ID;
	}

	/**
	 * One row of the list. Wire field order: {@code ownerUuid}, {@code ownerName} (resolved via
	 * the server's user cache, falls back to the raw UUID string if unresolved), {@code size},
	 * {@code maxSize} (from {@code PermissionProvider#getHighestSizeAllowed}), {@code type} (island
	 * type id), {@code currentBiomeId} (a LIVE lookup of the biome at the island's center block,
	 * not the persisted {@code IslandData#currentBiomeId} tracking field — see
	 * {@link AdminIslandBuilder}), {@code state}, {@code memberCount} (MEMBER/TRUSTED roles only,
	 * owner not counted — same filter {@code IslandMessages}/{@code IslandSnapshotBuilder} already
	 * apply to member lists), {@code isSpawnIsland} (NEW — true when {@code ownerUuid} is
	 * {@code Island.SERVER_OWNER_UUID}; see {@link AdminIslandBuilder#buildList} for how this row
	 * gets prepended ahead of pagination on page 0 with no active search).
	 */
	public record IslandEntry(
			UUID ownerUuid,
			String ownerName,
			int size,
			int maxSize,
			String type,
			String currentBiomeId,
			String state,
			int memberCount,
			boolean isSpawnIsland
	) {
		public static final PacketCodec<RegistryByteBuf, IslandEntry> CODEC = PacketCodec.of(
				(value, buf) -> {
					Uuids.PACKET_CODEC.encode(buf, value.ownerUuid());
					PacketCodecs.STRING.encode(buf, value.ownerName());
					PacketCodecs.VAR_INT.encode(buf, value.size());
					PacketCodecs.VAR_INT.encode(buf, value.maxSize());
					PacketCodecs.STRING.encode(buf, value.type());
					PacketCodecs.STRING.encode(buf, value.currentBiomeId());
					PacketCodecs.STRING.encode(buf, value.state());
					PacketCodecs.VAR_INT.encode(buf, value.memberCount());
					PacketCodecs.BOOL.encode(buf, value.isSpawnIsland());
				},
				buf -> new IslandEntry(
						Uuids.PACKET_CODEC.decode(buf),
						PacketCodecs.STRING.decode(buf),
						PacketCodecs.VAR_INT.decode(buf),
						PacketCodecs.VAR_INT.decode(buf),
						PacketCodecs.STRING.decode(buf),
						PacketCodecs.STRING.decode(buf),
						PacketCodecs.STRING.decode(buf),
						PacketCodecs.VAR_INT.decode(buf),
						PacketCodecs.BOOL.decode(buf)
				)
		);
	}
}
