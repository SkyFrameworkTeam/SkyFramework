package com.skyframework.islandcore.net.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// settingId matches IslandSetting#getId() (the same string IslandCommand's "settings" argument
// already accepts, e.g. "firespread"/"pvp"/"mobdamage") — resolved server-side the same way, via
// IslandSetting.fromId(settingId).
public record IslandSettingsUpdateC2S(String settingId, boolean value) implements CustomPayload {
	public static final CustomPayload.Id<IslandSettingsUpdateC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.ISLAND_SETTINGS_UPDATE_C2S);

	public static final PacketCodec<RegistryByteBuf, IslandSettingsUpdateC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, IslandSettingsUpdateC2S::settingId,
			PacketCodecs.BOOL, IslandSettingsUpdateC2S::value,
			IslandSettingsUpdateC2S::new
	);

	@Override
	public CustomPayload.Id<IslandSettingsUpdateC2S> getId() {
		return ID;
	}
}
