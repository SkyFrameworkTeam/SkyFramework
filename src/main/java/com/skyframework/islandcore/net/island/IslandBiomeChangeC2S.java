package com.skyframework.islandcore.net.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// biomeId is the raw Identifier string (e.g. "minecraft:jungle"), parsed server-side with
// Identifier.of(...) the same way IdentifierArgumentType already hands one to executeBiome — a
// malformed string is treated as ActionReason.BIOME_NOT_FOUND rather than crashing the handler.
public record IslandBiomeChangeC2S(String biomeId) implements CustomPayload {
	public static final CustomPayload.Id<IslandBiomeChangeC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.ISLAND_BIOME_CHANGE_C2S);

	public static final PacketCodec<RegistryByteBuf, IslandBiomeChangeC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, IslandBiomeChangeC2S::biomeId,
			IslandBiomeChangeC2S::new
	);

	@Override
	public CustomPayload.Id<IslandBiomeChangeC2S> getId() {
		return ID;
	}
}
