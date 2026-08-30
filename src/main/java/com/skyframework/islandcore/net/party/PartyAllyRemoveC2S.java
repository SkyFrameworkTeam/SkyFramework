package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// targetPartyName instead of a UUID: same reasoning as PartyAllyAddC2S.
public record PartyAllyRemoveC2S(String targetPartyName) implements CustomPayload {
	public static final CustomPayload.Id<PartyAllyRemoveC2S> ID = new CustomPayload.Id<>(NetworkChannels.PARTY_ALLY_REMOVE_C2S);

	public static final PacketCodec<RegistryByteBuf, PartyAllyRemoveC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, PartyAllyRemoveC2S::targetPartyName,
			PartyAllyRemoveC2S::new
	);

	@Override
	public CustomPayload.Id<PartyAllyRemoveC2S> getId() {
		return ID;
	}
}
