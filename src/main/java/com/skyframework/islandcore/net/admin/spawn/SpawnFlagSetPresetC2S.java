package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Network equivalent of a normal island's own FlagSetPresetC2S, targeting the Spawn island instead
// of the sender's own (see IslandCoreMod.ISLAND_REGISTRY.applyFlagPreset in
// ServerPacketHandlers#registerSpawnAdminHandlers) — operator-gated instead of ownership-gated.
// Wire field order: flagId, preset ("nadie"/"miembros"/"aliados"/"todos").
public record SpawnFlagSetPresetC2S(String flagId, String preset) implements CustomPayload {
	public static final CustomPayload.Id<SpawnFlagSetPresetC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.SPAWN_FLAG_SET_PRESET_C2S);

	public static final PacketCodec<RegistryByteBuf, SpawnFlagSetPresetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, SpawnFlagSetPresetC2S::flagId,
			PacketCodecs.STRING, SpawnFlagSetPresetC2S::preset,
			SpawnFlagSetPresetC2S::new
	);

	@Override
	public CustomPayload.Id<SpawnFlagSetPresetC2S> getId() {
		return ID;
	}
}
