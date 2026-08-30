package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// targetPartyName instead of a UUID: parties (unlike players) have no client-side UUID cache to
// pick from outside of PartyStatusS2C#alliedParties (which is the OUTPUT of this action, not an
// input source) — resolved server-side by PartyRegistry#getPartyByName, same as "/party ally add".
public record PartyAllyAddC2S(String targetPartyName) implements CustomPayload {
	public static final CustomPayload.Id<PartyAllyAddC2S> ID = new CustomPayload.Id<>(NetworkChannels.PARTY_ALLY_ADD_C2S);

	public static final PacketCodec<RegistryByteBuf, PartyAllyAddC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, PartyAllyAddC2S::targetPartyName,
			PartyAllyAddC2S::new
	);

	@Override
	public CustomPayload.Id<PartyAllyAddC2S> getId() {
		return ID;
	}
}
