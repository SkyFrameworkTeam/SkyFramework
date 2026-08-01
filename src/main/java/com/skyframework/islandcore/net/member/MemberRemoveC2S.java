package com.skyframework.islandcore.net.member;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

// Covers both /island untrust and /island kick in one action: MembershipService#removeMember
// decides which behavior applies from the target's current role (see that method's own doc for
// the reasoning). The client only needs one "remove this member" button, not two.
public record MemberRemoveC2S(UUID targetUuid) implements CustomPayload {
	public static final CustomPayload.Id<MemberRemoveC2S> ID = new CustomPayload.Id<>(NetworkChannels.MEMBER_REMOVE_C2S);

	public static final PacketCodec<RegistryByteBuf, MemberRemoveC2S> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, MemberRemoveC2S::targetUuid,
			MemberRemoveC2S::new
	);

	@Override
	public CustomPayload.Id<MemberRemoveC2S> getId() {
		return ID;
	}
}
