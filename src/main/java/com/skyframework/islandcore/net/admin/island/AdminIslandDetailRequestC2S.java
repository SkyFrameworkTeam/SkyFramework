package com.skyframework.islandcore.net.admin.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

// targetUuid is the island OWNER's uuid (same lookup key as /island admin list <player>), not the
// islandId — the admin-facing list/detail flow only ever knows players, matching how
// IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner is already used by the equivalent text commands.
public record AdminIslandDetailRequestC2S(UUID targetUuid) implements CustomPayload {

	public static final CustomPayload.Id<AdminIslandDetailRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.ADMIN_ISLAND_DETAIL_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, AdminIslandDetailRequestC2S> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, AdminIslandDetailRequestC2S::targetUuid,
			AdminIslandDetailRequestC2S::new
	);

	@Override
	public CustomPayload.Id<AdminIslandDetailRequestC2S> getId() {
		return ID;
	}
}
