package com.skyframework.islandcore.net.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose, like IslandSnapshotRequestC2S: the server acts on context.player(), never on
// client-supplied data.
public record IslandUpgradeC2S() implements CustomPayload {
	public static final CustomPayload.Id<IslandUpgradeC2S> ID = new CustomPayload.Id<>(NetworkChannels.ISLAND_UPGRADE_C2S);

	public static final PacketCodec<RegistryByteBuf, IslandUpgradeC2S> CODEC = PacketCodec.unit(new IslandUpgradeC2S());

	@Override
	public CustomPayload.Id<IslandUpgradeC2S> getId() {
		return ID;
	}
}
