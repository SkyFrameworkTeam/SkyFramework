package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose: the server acts on context.player(), same as MemberInviteAcceptC2S.
public record PartyAcceptC2S() implements CustomPayload {
	public static final CustomPayload.Id<PartyAcceptC2S> ID = new CustomPayload.Id<>(NetworkChannels.PARTY_ACCEPT_C2S);

	public static final PacketCodec<RegistryByteBuf, PartyAcceptC2S> CODEC =
			PacketCodec.unit(new PartyAcceptC2S());

	@Override
	public CustomPayload.Id<PartyAcceptC2S> getId() {
		return ID;
	}
}
