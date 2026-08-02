package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.Optional;

// Network equivalent of "/dimension regenerate <id> [seed]": calls the same
// DimensionRegistry#requestRegeneration(id, senderUuid, newSeed) the text command uses. seed
// absent means "random", same as DimensionCreateC2S.
public record DimensionRegenerateC2S(String id, Optional<Long> seed) implements CustomPayload {

	public static final CustomPayload.Id<DimensionRegenerateC2S> ID = new CustomPayload.Id<>(NetworkChannels.DIMENSION_REGENERATE_C2S);

	private static final PacketCodec<ByteBuf, Optional<Long>> SEED_CODEC = PacketCodecs.optional(PacketCodecs.VAR_LONG);

	public static final PacketCodec<RegistryByteBuf, DimensionRegenerateC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, DimensionRegenerateC2S::id,
			SEED_CODEC, DimensionRegenerateC2S::seed,
			DimensionRegenerateC2S::new
	);

	@Override
	public CustomPayload.Id<DimensionRegenerateC2S> getId() {
		return ID;
	}
}
