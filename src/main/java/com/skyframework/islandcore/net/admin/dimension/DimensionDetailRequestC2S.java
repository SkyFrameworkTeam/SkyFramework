package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// id is the dimension's path only (e.g. "foo" for "islandcore:foo"), matching the "id" argument
// DimensionCommand's Brigadier tree already takes — the handler builds the full Identifier the
// same way DimensionCommand#executeInfo does (Identifier.of("islandcore", id)).
public record DimensionDetailRequestC2S(String id) implements CustomPayload {

	public static final CustomPayload.Id<DimensionDetailRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.DIMENSION_DETAIL_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, DimensionDetailRequestC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, DimensionDetailRequestC2S::id,
			DimensionDetailRequestC2S::new
	);

	@Override
	public CustomPayload.Id<DimensionDetailRequestC2S> getId() {
		return ID;
	}
}
