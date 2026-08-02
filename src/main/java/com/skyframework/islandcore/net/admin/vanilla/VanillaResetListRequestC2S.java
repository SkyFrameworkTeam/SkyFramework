package com.skyframework.islandcore.net.admin.vanilla;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose: the server acts on VanillaResetService#listPendingResets(), never on
// client-supplied data.
public record VanillaResetListRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<VanillaResetListRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.VANILLA_RESET_LIST_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, VanillaResetListRequestC2S> CODEC =
			PacketCodec.unit(new VanillaResetListRequestC2S());

	@Override
	public CustomPayload.Id<VanillaResetListRequestC2S> getId() {
		return ID;
	}
}
