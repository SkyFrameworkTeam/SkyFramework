package com.skyframework.islandcore.net.flag;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Wire field order: flagId, value (raw string, parsed server-side the same way "/island flags
// set" parses its own "value" argument: TriState.valueOf(value.toUpperCase()) — allow/deny/default).
public record FlagSetC2S(String flagId, String value) implements CustomPayload {
	public static final CustomPayload.Id<FlagSetC2S> ID = new CustomPayload.Id<>(NetworkChannels.FLAG_SET_C2S);

	public static final PacketCodec<RegistryByteBuf, FlagSetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, FlagSetC2S::flagId,
			PacketCodecs.STRING, FlagSetC2S::value,
			FlagSetC2S::new
	);

	@Override
	public CustomPayload.Id<FlagSetC2S> getId() {
		return ID;
	}
}
