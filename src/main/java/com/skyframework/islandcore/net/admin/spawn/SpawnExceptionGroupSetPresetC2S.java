package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Network equivalent of "/island admin spawn exceptions preset <group> <preset>"
// (executeAdminSpawnExceptionsPreset) — same IslandCoreMod.ISLAND_REGISTRY.applyExceptionGroupPreset
// call, reachable from SpawnManagerScreen instead of only the command. Wire field order: groupId,
// preset ("nadie"/"miembros"/"aliados"/"todos").
public record SpawnExceptionGroupSetPresetC2S(String groupId, String preset) implements CustomPayload {
	public static final CustomPayload.Id<SpawnExceptionGroupSetPresetC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.SPAWN_EXCEPTION_GROUP_SET_PRESET_C2S);

	public static final PacketCodec<RegistryByteBuf, SpawnExceptionGroupSetPresetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, SpawnExceptionGroupSetPresetC2S::groupId,
			PacketCodecs.STRING, SpawnExceptionGroupSetPresetC2S::preset,
			SpawnExceptionGroupSetPresetC2S::new
	);

	@Override
	public CustomPayload.Id<SpawnExceptionGroupSetPresetC2S> getId() {
		return ID;
	}
}
