package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose. Arms the same 15s confirmation window as "/party disband" (see
// PartyDisbandRequests) — an actual disband only happens once PartyDisbandConfirmC2S follows
// within that window.
public record PartyDisbandRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<PartyDisbandRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.PARTY_DISBAND_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, PartyDisbandRequestC2S> CODEC =
			PacketCodec.unit(new PartyDisbandRequestC2S());

	@Override
	public CustomPayload.Id<PartyDisbandRequestC2S> getId() {
		return ID;
	}
}
