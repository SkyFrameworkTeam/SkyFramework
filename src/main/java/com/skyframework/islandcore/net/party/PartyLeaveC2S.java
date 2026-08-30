package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose: the server acts on context.player().
public record PartyLeaveC2S() implements CustomPayload {
	public static final CustomPayload.Id<PartyLeaveC2S> ID = new CustomPayload.Id<>(NetworkChannels.PARTY_LEAVE_C2S);

	public static final PacketCodec<RegistryByteBuf, PartyLeaveC2S> CODEC =
			PacketCodec.unit(new PartyLeaveC2S());

	@Override
	public CustomPayload.Id<PartyLeaveC2S> getId() {
		return ID;
	}
}
