package com.skyframework.islandcore.net.flag;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Replaces the old boolean-shaped ExceptionGroupSetC2S — exception groups now resolve per role,
// exact mirror of FlagSetPresetC2S. Wire field order: groupId, preset ("nadie"/"miembros"/
// "aliados"/"todos"). Only valid for owner-configurable groups.
public record ExceptionGroupSetPresetC2S(String groupId, String preset) implements CustomPayload {
	public static final CustomPayload.Id<ExceptionGroupSetPresetC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.EXCEPTION_GROUP_SET_PRESET_C2S);

	public static final PacketCodec<RegistryByteBuf, ExceptionGroupSetPresetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, ExceptionGroupSetPresetC2S::groupId,
			PacketCodecs.STRING, ExceptionGroupSetPresetC2S::preset,
			ExceptionGroupSetPresetC2S::new
	);

	@Override
	public CustomPayload.Id<ExceptionGroupSetPresetC2S> getId() {
		return ID;
	}
}
