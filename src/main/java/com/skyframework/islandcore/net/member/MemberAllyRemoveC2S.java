package com.skyframework.islandcore.net.member;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

// targetUuid instead of a name: unlike MemberAllyAddC2S, removing an ally always targets an
// existing ALLY entry the client already has a UUID for from its own IslandSnapshotS2C member
// list — same reasoning as MemberTrustC2S/MemberRemoveC2S.
public record MemberAllyRemoveC2S(UUID targetUuid) implements CustomPayload {
	public static final CustomPayload.Id<MemberAllyRemoveC2S> ID = new CustomPayload.Id<>(NetworkChannels.MEMBER_ALLY_REMOVE_C2S);

	public static final PacketCodec<RegistryByteBuf, MemberAllyRemoveC2S> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, MemberAllyRemoveC2S::targetUuid,
			MemberAllyRemoveC2S::new
	);

	@Override
	public CustomPayload.Id<MemberAllyRemoveC2S> getId() {
		return ID;
	}
}
