package com.skyframework.islandcore.net.flag;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Wire field order: groupId, enabled.
public record ExceptionGroupSetC2S(String groupId, boolean enabled) implements CustomPayload {
	public static final CustomPayload.Id<ExceptionGroupSetC2S> ID = new CustomPayload.Id<>(NetworkChannels.EXCEPTION_GROUP_SET_C2S);

	public static final PacketCodec<RegistryByteBuf, ExceptionGroupSetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, ExceptionGroupSetC2S::groupId,
			PacketCodecs.BOOL, ExceptionGroupSetC2S::enabled,
			ExceptionGroupSetC2S::new
	);

	@Override
	public CustomPayload.Id<ExceptionGroupSetC2S> getId() {
		return ID;
	}
}
