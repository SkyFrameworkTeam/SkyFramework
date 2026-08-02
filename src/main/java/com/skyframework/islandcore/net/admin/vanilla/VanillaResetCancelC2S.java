package com.skyframework.islandcore.net.admin.vanilla;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Network equivalent of "/dimension vanilla cancel <dimensionKey>": calls the same
// VanillaResetService#cancelPendingReset(dimensionKey) the text command uses. Cancels a QUEUED
// (already-confirmed) reset only — a still-counting-down PENDING_CONFIRMATION one is cancelled by
// simply letting the 30s window expire, same as every other confirmation flow in this codebase.
public record VanillaResetCancelC2S(String dimension) implements CustomPayload {

	public static final CustomPayload.Id<VanillaResetCancelC2S> ID = new CustomPayload.Id<>(NetworkChannels.VANILLA_RESET_CANCEL_C2S);

	public static final PacketCodec<RegistryByteBuf, VanillaResetCancelC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, VanillaResetCancelC2S::dimension,
			VanillaResetCancelC2S::new
	);

	@Override
	public CustomPayload.Id<VanillaResetCancelC2S> getId() {
		return ID;
	}
}
