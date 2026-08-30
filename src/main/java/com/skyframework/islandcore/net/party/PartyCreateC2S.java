package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Wire field order: name.
public record PartyCreateC2S(String name) implements CustomPayload {
	public static final CustomPayload.Id<PartyCreateC2S> ID = new CustomPayload.Id<>(NetworkChannels.PARTY_CREATE_C2S);

	public static final PacketCodec<RegistryByteBuf, PartyCreateC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, PartyCreateC2S::name,
			PartyCreateC2S::new
	);

	@Override
	public CustomPayload.Id<PartyCreateC2S> getId() {
		return ID;
	}
}
