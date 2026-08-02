package com.skyframework.islandcore.net.admin.island;

import com.skyframework.islandcore.net.NetworkChannels;
import com.skyframework.islandcore.net.island.IslandSnapshotS2C;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Same level of detail as {@code /island admin list <player>} ({@code IslandMessages
 * #sendIslandSummaryAdmin}), for the Admin Island Manager's detail screen. Field count is past
 * {@link PacketCodec#tuple}'s 6-argument limit (like {@link IslandSnapshotS2C}), so {@link #CODEC}
 * is hand-written with {@link PacketCodec#of}.
 *
 * <p>Wire field order: {@code islandId}, {@code ownerUuid}, {@code ownerName} (resolved via the
 * server's user cache, falls back to the raw UUID string), {@code dimension} (the registry key's
 * Identifier as a String), {@code gridX}, {@code gridZ}, {@code center}, {@code boundsMin},
 * {@code boundsMax}, {@code plotBoundsMin}, {@code plotBoundsMax}, {@code islandSize},
 * {@code maxSize} (from {@code PermissionProvider#getHighestSizeAllowed}, same computation
 * {@code AdminIslandBuilder} already uses for the list — placed right after {@code islandSize} to
 * mirror {@link AdminIslandListS2C.IslandEntry}'s adjacent {@code size}/{@code maxSize} pairing),
 * {@code plotSize}, {@code islandType} (type id), {@code homeLocation}, {@code members} (list of
 * {@link IslandSnapshotS2C.MemberEntry} — reused as-is, owner included with role
 * {@code "OWNER"}, same as {@code IslandSnapshotBuilder}), {@code state}, {@code createdAt} /
 * {@code updatedAt} (already formatted human-readable strings, {@code dd/MM/yyyy HH:mm} — same
 * formatting {@code IslandMessages} itself uses, done server-side so the client never needs its
 * own date formatting logic), {@code entities} (reuses {@link IslandSnapshotS2C.EntityCounts}).
 */
public record AdminIslandDetailS2C(
		UUID islandId,
		UUID ownerUuid,
		String ownerName,
		String dimension,
		int gridX,
		int gridZ,
		BlockPos center,
		BlockPos boundsMin,
		BlockPos boundsMax,
		BlockPos plotBoundsMin,
		BlockPos plotBoundsMax,
		int islandSize,
		int maxSize,
		int plotSize,
		String islandType,
		BlockPos homeLocation,
		List<IslandSnapshotS2C.MemberEntry> members,
		String state,
		String createdAt,
		String updatedAt,
		IslandSnapshotS2C.EntityCounts entities
) implements CustomPayload {

	public static final CustomPayload.Id<AdminIslandDetailS2C> ID = new CustomPayload.Id<>(NetworkChannels.ADMIN_ISLAND_DETAIL_S2C);

	private static final PacketCodec<RegistryByteBuf, List<IslandSnapshotS2C.MemberEntry>> MEMBER_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, IslandSnapshotS2C.MemberEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, AdminIslandDetailS2C> CODEC = PacketCodec.of(
			(value, buf) -> {
				Uuids.PACKET_CODEC.encode(buf, value.islandId());
				Uuids.PACKET_CODEC.encode(buf, value.ownerUuid());
				PacketCodecs.STRING.encode(buf, value.ownerName());
				PacketCodecs.STRING.encode(buf, value.dimension());
				PacketCodecs.VAR_INT.encode(buf, value.gridX());
				PacketCodecs.VAR_INT.encode(buf, value.gridZ());
				BlockPos.PACKET_CODEC.encode(buf, value.center());
				BlockPos.PACKET_CODEC.encode(buf, value.boundsMin());
				BlockPos.PACKET_CODEC.encode(buf, value.boundsMax());
				BlockPos.PACKET_CODEC.encode(buf, value.plotBoundsMin());
				BlockPos.PACKET_CODEC.encode(buf, value.plotBoundsMax());
				PacketCodecs.VAR_INT.encode(buf, value.islandSize());
				PacketCodecs.VAR_INT.encode(buf, value.maxSize());
				PacketCodecs.VAR_INT.encode(buf, value.plotSize());
				PacketCodecs.STRING.encode(buf, value.islandType());
				BlockPos.PACKET_CODEC.encode(buf, value.homeLocation());
				MEMBER_LIST_CODEC.encode(buf, value.members());
				PacketCodecs.STRING.encode(buf, value.state());
				PacketCodecs.STRING.encode(buf, value.createdAt());
				PacketCodecs.STRING.encode(buf, value.updatedAt());
				IslandSnapshotS2C.EntityCounts.CODEC.encode(buf, value.entities());
			},
			buf -> new AdminIslandDetailS2C(
					Uuids.PACKET_CODEC.decode(buf),
					Uuids.PACKET_CODEC.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.VAR_INT.decode(buf),
					PacketCodecs.VAR_INT.decode(buf),
					BlockPos.PACKET_CODEC.decode(buf),
					BlockPos.PACKET_CODEC.decode(buf),
					BlockPos.PACKET_CODEC.decode(buf),
					BlockPos.PACKET_CODEC.decode(buf),
					BlockPos.PACKET_CODEC.decode(buf),
					PacketCodecs.VAR_INT.decode(buf),
					PacketCodecs.VAR_INT.decode(buf),
					PacketCodecs.VAR_INT.decode(buf),
					PacketCodecs.STRING.decode(buf),
					BlockPos.PACKET_CODEC.decode(buf),
					MEMBER_LIST_CODEC.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.STRING.decode(buf),
					IslandSnapshotS2C.EntityCounts.CODEC.decode(buf)
			)
	);

	@Override
	public CustomPayload.Id<AdminIslandDetailS2C> getId() {
		return ID;
	}
}
