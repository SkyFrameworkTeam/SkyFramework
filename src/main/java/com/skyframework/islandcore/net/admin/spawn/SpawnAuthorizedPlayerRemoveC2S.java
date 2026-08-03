package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

// targetUuid instead of a name: unlike add (which may target an offline player the client only
// knows by name), remove always targets an existing entry from the authorizedPlayers list the
// client already fetched via SpawnBuildProtectionStatusS2C — same reasoning as MemberTrustC2S.
public record SpawnAuthorizedPlayerRemoveC2S(UUID targetUuid) implements CustomPayload {

	public static final CustomPayload.Id<SpawnAuthorizedPlayerRemoveC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.SPAWN_AUTHORIZED_PLAYER_REMOVE_C2S);

	public static final PacketCodec<RegistryByteBuf, SpawnAuthorizedPlayerRemoveC2S> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, SpawnAuthorizedPlayerRemoveC2S::targetUuid,
			SpawnAuthorizedPlayerRemoveC2S::new
	);

	@Override
	public CustomPayload.Id<SpawnAuthorizedPlayerRemoveC2S> getId() {
		return ID;
	}
}
