package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Same fields "/dimension info &lt;id&gt;" prints. Wire field order: {@code id} (full Identifier
 * as a String), {@code displayName}, {@code style}, {@code seed}, {@code state},
 * {@code createdAt} / {@code updatedAt} (already formatted human-readable strings, same
 * formatting as {@link com.skyframework.islandcore.net.admin.island.AdminIslandDetailS2C}).
 *
 * <p>Sent only on success — a target not found or a not-yet-decided style/id error replies with
 * {@code ActionResultS2C.fail(...)} instead (see {@code ActionReason#DIMENSION_NOT_FOUND}).
 */
public record DimensionDetailS2C(
		String id,
		String displayName,
		String style,
		long seed,
		String state,
		String createdAt,
		String updatedAt
) implements CustomPayload {

	public static final CustomPayload.Id<DimensionDetailS2C> ID = new CustomPayload.Id<>(NetworkChannels.DIMENSION_DETAIL_S2C);

	// 7 fields is past PacketCodec#tuple's 6-argument limit (confirmed against the decompiled
	// PacketCodec source, same reasoning as IslandSnapshotS2C/AdminIslandDetailS2C), so this is
	// hand-written with PacketCodec#of instead.
	public static final PacketCodec<RegistryByteBuf, DimensionDetailS2C> CODEC = PacketCodec.of(
			(value, buf) -> {
				PacketCodecs.STRING.encode(buf, value.id());
				PacketCodecs.STRING.encode(buf, value.displayName());
				PacketCodecs.STRING.encode(buf, value.style());
				PacketCodecs.VAR_LONG.encode(buf, value.seed());
				PacketCodecs.STRING.encode(buf, value.state());
				PacketCodecs.STRING.encode(buf, value.createdAt());
				PacketCodecs.STRING.encode(buf, value.updatedAt());
			},
			buf -> new DimensionDetailS2C(
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.VAR_LONG.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.STRING.decode(buf),
					PacketCodecs.STRING.decode(buf)
			)
	);

	@Override
	public CustomPayload.Id<DimensionDetailS2C> getId() {
		return ID;
	}
}
