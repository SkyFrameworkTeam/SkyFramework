package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose, like IslandSnapshotRequestC2S/IslandDeleteRequestC2S: the server acts on
// IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID), never on
// client-supplied data.
public record SpawnStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<SpawnStatusRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.SPAWN_STATUS_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, SpawnStatusRequestC2S> CODEC =
			PacketCodec.unit(new SpawnStatusRequestC2S());

	@Override
	public CustomPayload.Id<SpawnStatusRequestC2S> getId() {
		return ID;
	}
}
