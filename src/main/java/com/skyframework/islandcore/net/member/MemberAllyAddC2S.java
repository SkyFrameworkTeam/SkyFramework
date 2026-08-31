package com.skyframework.islandcore.net.member;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// targetName instead of a UUID: an ally, unlike trust/remove's targets, doesn't have to already be
// a member the client has a UUID for from its own IslandSnapshotS2C member list — same reasoning
// as MemberInviteC2S. Resolved server-side by MembershipService#resolvePlayerUuid, the exact same
// name-resolution mechanism MembershipService#inviteByName already reuses for MemberInviteC2S.
public record MemberAllyAddC2S(String targetName) implements CustomPayload {
	public static final CustomPayload.Id<MemberAllyAddC2S> ID = new CustomPayload.Id<>(NetworkChannels.MEMBER_ALLY_ADD_C2S);

	public static final PacketCodec<RegistryByteBuf, MemberAllyAddC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, MemberAllyAddC2S::targetName,
			MemberAllyAddC2S::new
	);

	@Override
	public CustomPayload.Id<MemberAllyAddC2S> getId() {
		return ID;
	}
}
