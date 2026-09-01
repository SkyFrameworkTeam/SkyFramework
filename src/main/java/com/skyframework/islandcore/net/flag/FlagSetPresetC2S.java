package com.skyframework.islandcore.net.flag;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Wire field order: flagId, preset ("nadie"/"miembros"/"aliados"/"todos" —
// see protection.flag.FlagPreset). Only valid for ROLE_BASED flags; the server validates both and
// reports ActionReason.INVALID_FLAG_PRESET for either mismatch — see IslandActionService#applyFlagPreset.
public record FlagSetPresetC2S(String flagId, String preset) implements CustomPayload {
	public static final CustomPayload.Id<FlagSetPresetC2S> ID = new CustomPayload.Id<>(NetworkChannels.FLAG_SET_PRESET_C2S);

	public static final PacketCodec<RegistryByteBuf, FlagSetPresetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, FlagSetPresetC2S::flagId,
			PacketCodecs.STRING, FlagSetPresetC2S::preset,
			FlagSetPresetC2S::new
	);

	@Override
	public CustomPayload.Id<FlagSetPresetC2S> getId() {
		return ID;
	}
}
