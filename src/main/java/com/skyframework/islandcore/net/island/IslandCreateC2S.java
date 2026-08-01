package com.skyframework.islandcore.net.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose, like IslandSnapshotRequestC2S: the server acts on context.player(), never on
// client-supplied data.
public record IslandCreateC2S() implements CustomPayload {
	public static final CustomPayload.Id<IslandCreateC2S> ID = new CustomPayload.Id<>(NetworkChannels.ISLAND_CREATE_C2S);

	public static final PacketCodec<RegistryByteBuf, IslandCreateC2S> CODEC = PacketCodec.unit(new IslandCreateC2S());

	@Override
	public CustomPayload.Id<IslandCreateC2S> getId() {
		return ID;
	}
}
