package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// targetName instead of a UUID: like MemberInviteC2S, the client has no reliable way to know an
// offline player's UUID up front. Resolved server-side by MembershipService#resolvePlayerUuid,
// which has no Brigadier GameProfileArgumentType to do this resolution for free like
// "/island admin spawn trust <player>" does.
public record SpawnAuthorizedPlayerAddC2S(String targetName) implements CustomPayload {

	public static final CustomPayload.Id<SpawnAuthorizedPlayerAddC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.SPAWN_AUTHORIZED_PLAYER_ADD_C2S);

	public static final PacketCodec<RegistryByteBuf, SpawnAuthorizedPlayerAddC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, SpawnAuthorizedPlayerAddC2S::targetName,
			SpawnAuthorizedPlayerAddC2S::new
	);

	@Override
	public CustomPayload.Id<SpawnAuthorizedPlayerAddC2S> getId() {
		return ID;
	}
}
