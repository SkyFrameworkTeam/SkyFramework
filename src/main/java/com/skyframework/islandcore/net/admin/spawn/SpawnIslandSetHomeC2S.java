package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose: no coordinates travel over the wire. The handler reads the ACTUAL sender's
// player.getBlockPos() at the moment the packet is received, applying the exact same
// dimension+bounds validation "/island admin spawn sethome" already does inline before calling
// IslandRegistry#updateHomeLocation — see ServerPacketHandlers for that check. Failure (not inside
// the Spawn island's built bounds) replies with ActionResultS2C.fail(ActionReason.UNSAFE_LOCATION).
public record SpawnIslandSetHomeC2S() implements CustomPayload {
	public static final CustomPayload.Id<SpawnIslandSetHomeC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.SPAWN_ISLAND_SET_HOME_C2S);

	public static final PacketCodec<RegistryByteBuf, SpawnIslandSetHomeC2S> CODEC =
			PacketCodec.unit(new SpawnIslandSetHomeC2S());

	@Override
	public CustomPayload.Id<SpawnIslandSetHomeC2S> getId() {
		return ID;
	}
}
