package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

// targetUuid instead of a name: the target is always an existing party member the client already
// has a UUID for from its own PartyStatusS2C member list — same reasoning as member.MemberTrustC2S.
public record PartyKickC2S(UUID targetUuid) implements CustomPayload {
	public static final CustomPayload.Id<PartyKickC2S> ID = new CustomPayload.Id<>(NetworkChannels.PARTY_KICK_C2S);

	public static final PacketCodec<RegistryByteBuf, PartyKickC2S> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, PartyKickC2S::targetUuid,
			PartyKickC2S::new
	);

	@Override
	public CustomPayload.Id<PartyKickC2S> getId() {
		return ID;
	}
}
