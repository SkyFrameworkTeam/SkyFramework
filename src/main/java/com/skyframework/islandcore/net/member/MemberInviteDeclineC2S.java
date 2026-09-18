package com.skyframework.islandcore.net.member;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose, like MemberInviteAcceptC2S: the server acts on context.player(), never on
// client-supplied data.
public record MemberInviteDeclineC2S() implements CustomPayload {
	public static final CustomPayload.Id<MemberInviteDeclineC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.MEMBER_INVITE_DECLINE_C2S);

	public static final PacketCodec<RegistryByteBuf, MemberInviteDeclineC2S> CODEC =
			PacketCodec.unit(new MemberInviteDeclineC2S());

	@Override
	public CustomPayload.Id<MemberInviteDeclineC2S> getId() {
		return ID;
	}
}
