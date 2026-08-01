package com.skyframework.islandcore.net.member;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// targetName instead of a UUID: the client has no reliable way to know an offline player's UUID
// up front. Resolved server-side by MembershipService#inviteByName, which has no Brigadier
// GameProfileArgumentType to do this resolution for free like the text command does.
public record MemberInviteC2S(String targetName) implements CustomPayload {
	public static final CustomPayload.Id<MemberInviteC2S> ID = new CustomPayload.Id<>(NetworkChannels.MEMBER_INVITE_C2S);

	public static final PacketCodec<RegistryByteBuf, MemberInviteC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, MemberInviteC2S::targetName,
			MemberInviteC2S::new
	);

	@Override
	public CustomPayload.Id<MemberInviteC2S> getId() {
		return ID;
	}
}
