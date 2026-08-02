package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Network equivalent of "/dimension delete <id> confirm": calls the same
// DimensionRegistry#confirmDeletion(id, senderUuid) the text command uses.
public record DimensionDeleteConfirmC2S(String id) implements CustomPayload {

	public static final CustomPayload.Id<DimensionDeleteConfirmC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.DIMENSION_DELETE_CONFIRM_C2S);

	public static final PacketCodec<RegistryByteBuf, DimensionDeleteConfirmC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, DimensionDeleteConfirmC2S::id,
			DimensionDeleteConfirmC2S::new
	);

	@Override
	public CustomPayload.Id<DimensionDeleteConfirmC2S> getId() {
		return ID;
	}
}
