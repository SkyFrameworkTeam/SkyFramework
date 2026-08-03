package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Network equivalent of "/island admin spawn settings buildprotection <value>": delegates to
// IslandRegistry#updateIslandSetting(spawnIslandId, IslandSetting.BUILD_PROTECTION, enabled), the
// same call the text command makes.
public record SpawnBuildProtectionSetC2S(boolean enabled) implements CustomPayload {

	public static final CustomPayload.Id<SpawnBuildProtectionSetC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.SPAWN_BUILD_PROTECTION_SET_C2S);

	public static final PacketCodec<RegistryByteBuf, SpawnBuildProtectionSetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL, SpawnBuildProtectionSetC2S::enabled,
			SpawnBuildProtectionSetC2S::new
	);

	@Override
	public CustomPayload.Id<SpawnBuildProtectionSetC2S> getId() {
		return ID;
	}
}
