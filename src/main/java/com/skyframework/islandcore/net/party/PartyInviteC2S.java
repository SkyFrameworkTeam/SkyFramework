package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// targetName instead of a UUID: same reasoning as member.MemberInviteC2S — the client has no
// reliable way to know an offline player's UUID up front. Resolved server-side by
// MembershipService#resolvePlayerUuid, the exact same name-resolution mechanism
// SpawnAuthorizedPlayerAddC2S already reuses.
public record PartyInviteC2S(String targetName) implements CustomPayload {
	public static final CustomPayload.Id<PartyInviteC2S> ID = new CustomPayload.Id<>(NetworkChannels.PARTY_INVITE_C2S);

	public static final PacketCodec<RegistryByteBuf, PartyInviteC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, PartyInviteC2S::targetName,
			PartyInviteC2S::new
	);

	@Override
	public CustomPayload.Id<PartyInviteC2S> getId() {
		return ID;
	}
}
