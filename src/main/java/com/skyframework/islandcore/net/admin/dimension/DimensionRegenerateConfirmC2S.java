package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Network equivalent of "/dimension regenerate <id> confirm": calls the same
// DimensionRegistry#confirmRegeneration(id, senderUuid) the text command uses.
public record DimensionRegenerateConfirmC2S(String id) implements CustomPayload {

	public static final CustomPayload.Id<DimensionRegenerateConfirmC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.DIMENSION_REGENERATE_CONFIRM_C2S);

	public static final PacketCodec<RegistryByteBuf, DimensionRegenerateConfirmC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, DimensionRegenerateConfirmC2S::id,
			DimensionRegenerateConfirmC2S::new
	);

	@Override
	public CustomPayload.Id<DimensionRegenerateConfirmC2S> getId() {
		return ID;
	}
}
