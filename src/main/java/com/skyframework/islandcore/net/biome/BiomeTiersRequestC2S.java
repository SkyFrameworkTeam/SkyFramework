package com.skyframework.islandcore.net.biome;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose, like IslandSnapshotRequestC2S: the server acts on context.player(), never on
// client-supplied data.
public record BiomeTiersRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<BiomeTiersRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.BIOME_TIERS_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, BiomeTiersRequestC2S> CODEC =
			PacketCodec.unit(new BiomeTiersRequestC2S());

	@Override
	public CustomPayload.Id<BiomeTiersRequestC2S> getId() {
		return ID;
	}
}
