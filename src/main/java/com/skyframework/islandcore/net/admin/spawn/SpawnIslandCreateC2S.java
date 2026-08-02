package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Network equivalent of "/island admin spawn create <size>": calls the same
// IslandRegistry#createSpawnIsland(ISLANDS_DIMENSION, size) the text command uses.
public record SpawnIslandCreateC2S(int size) implements CustomPayload {

	public static final CustomPayload.Id<SpawnIslandCreateC2S> ID = new CustomPayload.Id<>(NetworkChannels.SPAWN_ISLAND_CREATE_C2S);

	public static final PacketCodec<RegistryByteBuf, SpawnIslandCreateC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, SpawnIslandCreateC2S::size,
			SpawnIslandCreateC2S::new
	);

	@Override
	public CustomPayload.Id<SpawnIslandCreateC2S> getId() {
		return ID;
	}
}
