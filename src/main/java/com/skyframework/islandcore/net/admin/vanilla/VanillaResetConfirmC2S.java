package com.skyframework.islandcore.net.admin.vanilla;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Network equivalent of "/dimension vanilla regenerate <dimensionKey> confirm": calls the same
// VanillaResetService#confirmReset(dimensionKey, senderUuid) the text command uses.
public record VanillaResetConfirmC2S(String dimension) implements CustomPayload {

	public static final CustomPayload.Id<VanillaResetConfirmC2S> ID = new CustomPayload.Id<>(NetworkChannels.VANILLA_RESET_CONFIRM_C2S);

	public static final PacketCodec<RegistryByteBuf, VanillaResetConfirmC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, VanillaResetConfirmC2S::dimension,
			VanillaResetConfirmC2S::new
	);

	@Override
	public CustomPayload.Id<VanillaResetConfirmC2S> getId() {
		return ID;
	}
}
