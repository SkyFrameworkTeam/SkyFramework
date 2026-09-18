package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose, like SpawnStatusRequestC2S: the server acts on
// IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID), never on
// client-supplied data. Replies with the SAME FlagsStatusS2C a normal island's own
// FlagsStatusRequestC2S replies with (FlagsStatusBuilder#buildFlagsStatus already takes an Island
// directly, so it's fully reusable here) — see ServerPacketHandlers#registerSpawnAdminHandlers.
public record SpawnFlagsStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<SpawnFlagsStatusRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.SPAWN_FLAGS_STATUS_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, SpawnFlagsStatusRequestC2S> CODEC =
			PacketCodec.unit(new SpawnFlagsStatusRequestC2S());

	@Override
	public CustomPayload.Id<SpawnFlagsStatusRequestC2S> getId() {
		return ID;
	}
}
