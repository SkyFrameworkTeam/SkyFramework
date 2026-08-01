package com.skyframework.islandcore.net.member;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose, like IslandSnapshotRequestC2S: the server acts on context.player(), never on
// client-supplied data.
public record MemberInviteAcceptC2S() implements CustomPayload {
	public static final CustomPayload.Id<MemberInviteAcceptC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.MEMBER_INVITE_ACCEPT_C2S);

	public static final PacketCodec<RegistryByteBuf, MemberInviteAcceptC2S> CODEC =
			PacketCodec.unit(new MemberInviteAcceptC2S());

	@Override
	public CustomPayload.Id<MemberInviteAcceptC2S> getId() {
		return ID;
	}
}
