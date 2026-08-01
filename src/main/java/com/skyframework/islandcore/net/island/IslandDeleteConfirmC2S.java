package com.skyframework.islandcore.net.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose, like IslandSnapshotRequestC2S: the server acts on context.player(), never on
// client-supplied data.
public record IslandDeleteConfirmC2S() implements CustomPayload {
	public static final CustomPayload.Id<IslandDeleteConfirmC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.ISLAND_DELETE_CONFIRM_C2S);

	public static final PacketCodec<RegistryByteBuf, IslandDeleteConfirmC2S> CODEC =
			PacketCodec.unit(new IslandDeleteConfirmC2S());

	@Override
	public CustomPayload.Id<IslandDeleteConfirmC2S> getId() {
		return ID;
	}
}
