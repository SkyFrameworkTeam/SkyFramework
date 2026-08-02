package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Network equivalent of "/dimension delete <id>": calls the same
// DimensionRegistry#requestDeletion(id, senderUuid) the text command uses.
public record DimensionDeleteC2S(String id) implements CustomPayload {

	public static final CustomPayload.Id<DimensionDeleteC2S> ID = new CustomPayload.Id<>(NetworkChannels.DIMENSION_DELETE_C2S);

	public static final PacketCodec<RegistryByteBuf, DimensionDeleteC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, DimensionDeleteC2S::id,
			DimensionDeleteC2S::new
	);

	@Override
	public CustomPayload.Id<DimensionDeleteC2S> getId() {
		return ID;
	}
}
