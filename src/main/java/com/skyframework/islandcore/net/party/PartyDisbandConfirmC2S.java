package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose. Only succeeds if PartyDisbandRequestC2S (or "/party disband") armed the
// confirmation window within the last 15s — see PartyDisbandRequests.
public record PartyDisbandConfirmC2S() implements CustomPayload {
	public static final CustomPayload.Id<PartyDisbandConfirmC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.PARTY_DISBAND_CONFIRM_C2S);

	public static final PacketCodec<RegistryByteBuf, PartyDisbandConfirmC2S> CODEC =
			PacketCodec.unit(new PartyDisbandConfirmC2S());

	@Override
	public CustomPayload.Id<PartyDisbandConfirmC2S> getId() {
		return ID;
	}
}
