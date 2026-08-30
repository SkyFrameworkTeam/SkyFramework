package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose: the server identifies the requester from the connection itself
// (context.player()), same as IslandSnapshotRequestC2S.
public record PartyStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<PartyStatusRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.PARTY_STATUS_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, PartyStatusRequestC2S> CODEC =
			PacketCodec.unit(new PartyStatusRequestC2S());

	@Override
	public CustomPayload.Id<PartyStatusRequestC2S> getId() {
		return ID;
	}
}
