package com.skyframework.islandcore.net.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose: the server identifies the requester from the connection itself
// (context.player()), it never needs the client to tell it who's asking.
public record IslandSnapshotRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<IslandSnapshotRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.ISLAND_SNAPSHOT_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, IslandSnapshotRequestC2S> CODEC =
			PacketCodec.unit(new IslandSnapshotRequestC2S());

	@Override
	public CustomPayload.Id<IslandSnapshotRequestC2S> getId() {
		return ID;
	}
}
