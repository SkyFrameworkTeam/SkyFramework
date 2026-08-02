package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose: the server acts on DimensionRegistry#getAllDimensions(), never on
// client-supplied data. No pagination here (unlike the island list) — matches
// "/dimension list" itself, which doesn't paginate either.
public record DimensionListRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<DimensionListRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.DIMENSION_LIST_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, DimensionListRequestC2S> CODEC =
			PacketCodec.unit(new DimensionListRequestC2S());

	@Override
	public CustomPayload.Id<DimensionListRequestC2S> getId() {
		return ID;
	}
}
