package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Network equivalent of a normal island's own FlagSetC2S, targeting the Spawn island instead of the
// sender's own (see IslandCoreMod.ISLAND_REGISTRY.updateRoleFlagOverride/updateGlobalFlagOverride
// in ServerPacketHandlers#registerSpawnAdminHandlers) — operator-gated instead of
// ownership/FLAG_PERMISSION_REQUIREMENTS-gated, since IslandActionService#updateFlag resolves the
// island from the ACTING player's own UUID and can't be reused as-is for an admin acting on Spawn.
// Wire field order: flagId, value ("allow"/"deny"/"default", same TriState vocabulary FlagSetC2S
// already uses).
public record SpawnFlagSetC2S(String flagId, String value) implements CustomPayload {
	public static final CustomPayload.Id<SpawnFlagSetC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.SPAWN_FLAG_SET_C2S);

	public static final PacketCodec<RegistryByteBuf, SpawnFlagSetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, SpawnFlagSetC2S::flagId,
			PacketCodecs.STRING, SpawnFlagSetC2S::value,
			SpawnFlagSetC2S::new
	);

	@Override
	public CustomPayload.Id<SpawnFlagSetC2S> getId() {
		return ID;
	}
}
