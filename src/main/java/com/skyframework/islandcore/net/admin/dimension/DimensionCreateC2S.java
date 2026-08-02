package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.Optional;

// Network equivalent of "/dimension create <id> <displayName> <style> [seed]". id is the path
// only (see DimensionDetailRequestC2S); style is DimensionGeneratorStyle's name (case-insensitive
// on the handler side, matching DimensionCommand#executeCreate's own .toUpperCase(Locale.ROOT)).
// seed absent means "random", exactly like the text command omitting its optional seed argument.
public record DimensionCreateC2S(String id, String displayName, String style, Optional<Long> seed) implements CustomPayload {

	public static final CustomPayload.Id<DimensionCreateC2S> ID = new CustomPayload.Id<>(NetworkChannels.DIMENSION_CREATE_C2S);

	private static final PacketCodec<ByteBuf, Optional<Long>> SEED_CODEC = PacketCodecs.optional(PacketCodecs.VAR_LONG);

	public static final PacketCodec<RegistryByteBuf, DimensionCreateC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, DimensionCreateC2S::id,
			PacketCodecs.STRING, DimensionCreateC2S::displayName,
			PacketCodecs.STRING, DimensionCreateC2S::style,
			SEED_CODEC, DimensionCreateC2S::seed,
			DimensionCreateC2S::new
	);

	@Override
	public CustomPayload.Id<DimensionCreateC2S> getId() {
		return ID;
	}
}
