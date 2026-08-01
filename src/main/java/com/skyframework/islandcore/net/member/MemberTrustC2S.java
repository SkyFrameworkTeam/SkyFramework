package com.skyframework.islandcore.net.member;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

// targetUuid instead of a name: unlike invite (which may target an offline player the client
// only knows by name), trust always targets an existing member the client already has a UUID
// for from its own IslandSnapshotS2C member list.
public record MemberTrustC2S(UUID targetUuid) implements CustomPayload {
	public static final CustomPayload.Id<MemberTrustC2S> ID = new CustomPayload.Id<>(NetworkChannels.MEMBER_TRUST_C2S);

	public static final PacketCodec<RegistryByteBuf, MemberTrustC2S> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, MemberTrustC2S::targetUuid,
			MemberTrustC2S::new
	);

	@Override
	public CustomPayload.Id<MemberTrustC2S> getId() {
		return ID;
	}
}
