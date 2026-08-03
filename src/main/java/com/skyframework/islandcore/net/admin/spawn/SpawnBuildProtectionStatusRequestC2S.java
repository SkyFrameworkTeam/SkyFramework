package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose, like SpawnStatusRequestC2S: the server acts on
// IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID), never on
// client-supplied data.
public record SpawnBuildProtectionStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<SpawnBuildProtectionStatusRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.SPAWN_BUILD_PROTECTION_STATUS_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, SpawnBuildProtectionStatusRequestC2S> CODEC =
			PacketCodec.unit(new SpawnBuildProtectionStatusRequestC2S());

	@Override
	public CustomPayload.Id<SpawnBuildProtectionStatusRequestC2S> getId() {
		return ID;
	}
}
