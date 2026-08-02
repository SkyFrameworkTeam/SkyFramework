package com.skyframework.islandcore.net.admin.vanilla;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The full vanilla-reset queue ({@code pending_vanilla_reset.json}), same entries
 * "/dimension vanilla list" prints. Wire field order: {@code queue} (list of
 * {@link QueueEntry}).
 */
public record VanillaResetListS2C(List<QueueEntry> queue) implements CustomPayload {

	public static final CustomPayload.Id<VanillaResetListS2C> ID = new CustomPayload.Id<>(NetworkChannels.VANILLA_RESET_LIST_S2C);

	private static final PacketCodec<RegistryByteBuf, List<QueueEntry>> QUEUE_CODEC =
			PacketCodecs.collection(ArrayList::new, QueueEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, VanillaResetListS2C> CODEC = PacketCodec.tuple(
			QUEUE_CODEC, VanillaResetListS2C::queue,
			VanillaResetListS2C::new
	);

	@Override
	public CustomPayload.Id<VanillaResetListS2C> getId() {
		return ID;
	}

	/**
	 * Wire field order: {@code dimensionKey} ({@code "overworld"}/{@code "nether"}/{@code "end"}),
	 * {@code seed} (empty meaning "keeps the dimension's current seed"), {@code seedMode} (
	 * {@link com.skyframework.islandcore.dimension.vanilla.PendingVanillaReset.SeedMode} name —
	 * {@code "RANDOM"}/{@code "KEEP"}/{@code "CUSTOM"}, persisted verbatim from how the seed was
	 * actually decided at confirm time rather than re-derived from whether {@code seed} is present,
	 * since a resolved RANDOM seed and a CUSTOM one are otherwise indistinguishable once resolved),
	 * {@code requestedBy}, {@code status} ({@link com.skyframework.islandcore.dimension.vanilla.PendingVanillaReset.Status}
	 * name — always {@code "IN_PROGRESS"} today, since that's the only status ever persisted to
	 * the queue file, but sent as-is rather than hardcoded client-side in case that changes).
	 */
	public record QueueEntry(String dimensionKey, Optional<Long> seed, String seedMode, UUID requestedBy, String status) {
		private static final PacketCodec<ByteBuf, Optional<Long>> SEED_CODEC = PacketCodecs.optional(PacketCodecs.VAR_LONG);

		public static final PacketCodec<RegistryByteBuf, QueueEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, QueueEntry::dimensionKey,
				SEED_CODEC, QueueEntry::seed,
				PacketCodecs.STRING, QueueEntry::seedMode,
				Uuids.PACKET_CODEC, QueueEntry::requestedBy,
				PacketCodecs.STRING, QueueEntry::status,
				QueueEntry::new
		);
	}
}
