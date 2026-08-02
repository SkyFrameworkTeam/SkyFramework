package com.skyframework.islandcore.net.admin.vanilla;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.Optional;

/**
 * Network equivalent of "/dimension vanilla regenerate &lt;dimensionKey&gt; [seed]" (the initial
 * request, not the confirm) — calls the same {@code VanillaResetService#requestReset}. Wire field
 * order: {@code dimension} (one of {@code VanillaResetService.VALID_DIMENSION_KEYS}:
 * {@code "overworld"}/{@code "nether"}/{@code "end"}), {@code seedMode} (either
 * {@code "CUSTOM"} or {@code "DEFAULT"} — {@code requestReset} itself only ever takes a nullable
 * explicit seed, it has no such mode concept, so this is the network equivalent of "did the text
 * command's optional seed argument get supplied or not": {@code "CUSTOM"} maps to
 * {@code explicitSeed = seedValue}, anything else maps to {@code explicitSeed = null}, deferring
 * to the server's own {@code VanillaResetConfig} seed-mode default exactly like omitting the
 * argument does today), {@code seedValue} (only meaningful when {@code seedMode == "CUSTOM"}).
 */
public record VanillaResetQueueC2S(String dimension, String seedMode, Optional<Long> seedValue) implements CustomPayload {

	public static final CustomPayload.Id<VanillaResetQueueC2S> ID = new CustomPayload.Id<>(NetworkChannels.VANILLA_RESET_QUEUE_C2S);

	private static final PacketCodec<ByteBuf, Optional<Long>> SEED_CODEC = PacketCodecs.optional(PacketCodecs.VAR_LONG);

	public static final PacketCodec<RegistryByteBuf, VanillaResetQueueC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, VanillaResetQueueC2S::dimension,
			PacketCodecs.STRING, VanillaResetQueueC2S::seedMode,
			SEED_CODEC, VanillaResetQueueC2S::seedValue,
			VanillaResetQueueC2S::new
	);

	@Override
	public CustomPayload.Id<VanillaResetQueueC2S> getId() {
		return ID;
	}
}
