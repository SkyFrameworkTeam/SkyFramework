package com.skyframework.islandcore.net.island;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Full state of the receiving player's own island, or {@code exists = false} with default/empty
 * values in every other field if they don't have one yet. Built by {@link IslandSnapshotBuilder}
 * from the existing island services — this record only carries data, no business logic.
 *
 * <p>Field count (11) is past {@link PacketCodec#tuple}'s 6-argument limit, so {@link #CODEC} is
 * written by hand with {@link PacketCodec#of}; the nested per-entry records below stay within
 * the limit and keep their own small tuple codecs.
 *
 * <p><b>Wire format changed:</b> {@code currentBiomeId} was inserted after {@code type} — this is
 * a client/server protocol break for IslandCoreClient's own copy of this record, which needs the
 * matching update made separately in that project.
 */
public record IslandSnapshotS2C(
		boolean exists,
		int size,
		int maxSize,
		String type,
		String currentBiomeId,
		Optional<BlockPos> home,
		String state,
		List<MemberEntry> members,
		List<PendingInviteEntry> pendingInvites,
		List<SettingEntry> settings,
		EntityCounts entities
) implements CustomPayload {

	public static final CustomPayload.Id<IslandSnapshotS2C> ID = new CustomPayload.Id<>(NetworkChannels.ISLAND_SNAPSHOT_S2C);

	// PacketCodecs.optional(PacketCodec<B, V>) requires an exact B match (no "? super B" wildcard
	// like collection() below has), so this stays typed over plain ByteBuf rather than
	// RegistryByteBuf; encode/decode calls below still accept a RegistryByteBuf argument fine.
	private static final PacketCodec<ByteBuf, Optional<BlockPos>> HOME_CODEC = PacketCodecs.optional(BlockPos.PACKET_CODEC);
	private static final PacketCodec<RegistryByteBuf, List<MemberEntry>> MEMBER_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, MemberEntry.CODEC);
	private static final PacketCodec<RegistryByteBuf, List<PendingInviteEntry>> PENDING_INVITE_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, PendingInviteEntry.CODEC);
	private static final PacketCodec<RegistryByteBuf, List<SettingEntry>> SETTING_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, SettingEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, IslandSnapshotS2C> CODEC = PacketCodec.of(
			(value, buf) -> {
				PacketCodecs.BOOL.encode(buf, value.exists());
				PacketCodecs.VAR_INT.encode(buf, value.size());
				PacketCodecs.VAR_INT.encode(buf, value.maxSize());
				PacketCodecs.STRING.encode(buf, value.type());
				PacketCodecs.STRING.encode(buf, value.currentBiomeId());
				HOME_CODEC.encode(buf, value.home());
				PacketCodecs.STRING.encode(buf, value.state());
				MEMBER_LIST_CODEC.encode(buf, value.members());
				PENDING_INVITE_LIST_CODEC.encode(buf, value.pendingInvites());
				SETTING_LIST_CODEC.encode(buf, value.settings());
				EntityCounts.CODEC.encode(buf, value.entities());
			},
			buf -> new IslandSnapshotS2C(
					PacketCodecs.BOOL.decode(buf),
					PacketCodecs.VAR_INT.decode(buf),
					PacketCodecs.VAR_INT.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.STRING.decode(buf),
					HOME_CODEC.decode(buf),
					PacketCodecs.STRING.decode(buf),
					MEMBER_LIST_CODEC.decode(buf),
					PENDING_INVITE_LIST_CODEC.decode(buf),
					SETTING_LIST_CODEC.decode(buf),
					EntityCounts.CODEC.decode(buf)
			)
	);

	@Override
	public CustomPayload.Id<IslandSnapshotS2C> getId() {
		return ID;
	}

	public record MemberEntry(UUID uuid, String name, String role) {
		public static final PacketCodec<RegistryByteBuf, MemberEntry> CODEC = PacketCodec.tuple(
				net.minecraft.util.Uuids.PACKET_CODEC, MemberEntry::uuid,
				PacketCodecs.STRING, MemberEntry::name,
				PacketCodecs.STRING, MemberEntry::role,
				MemberEntry::new
		);
	}

	public record PendingInviteEntry(String targetName, int expiresInSeconds) {
		public static final PacketCodec<RegistryByteBuf, PendingInviteEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, PendingInviteEntry::targetName,
				PacketCodecs.VAR_INT, PendingInviteEntry::expiresInSeconds,
				PendingInviteEntry::new
		);
	}

	public record SettingEntry(String key, boolean value) {
		public static final PacketCodec<RegistryByteBuf, SettingEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, SettingEntry::key,
				PacketCodecs.BOOL, SettingEntry::value,
				SettingEntry::new
		);
	}

	public record EntityCounts(int players, int hostile, int passive, int cobblemon, int items, int other) {
		public static final PacketCodec<RegistryByteBuf, EntityCounts> CODEC = PacketCodec.tuple(
				PacketCodecs.VAR_INT, EntityCounts::players,
				PacketCodecs.VAR_INT, EntityCounts::hostile,
				PacketCodecs.VAR_INT, EntityCounts::passive,
				PacketCodecs.VAR_INT, EntityCounts::cobblemon,
				PacketCodecs.VAR_INT, EntityCounts::items,
				PacketCodecs.VAR_INT, EntityCounts::other,
				EntityCounts::new
		);

		public static final EntityCounts EMPTY = new EntityCounts(0, 0, 0, 0, 0, 0);
	}
}
