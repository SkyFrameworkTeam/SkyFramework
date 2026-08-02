package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Network equivalent of "/island admin spawn resize <size>": calls the same
// IslandRegistry#resizeIsland(spawnIslandId, newSize) the text command uses.
public record SpawnIslandResizeC2S(int newSize) implements CustomPayload {

	public static final CustomPayload.Id<SpawnIslandResizeC2S> ID = new CustomPayload.Id<>(NetworkChannels.SPAWN_ISLAND_RESIZE_C2S);

	public static final PacketCodec<RegistryByteBuf, SpawnIslandResizeC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, SpawnIslandResizeC2S::newSize,
			SpawnIslandResizeC2S::new
	);

	@Override
	public CustomPayload.Id<SpawnIslandResizeC2S> getId() {
		return ID;
	}
}
