package com.skyframework.islandcore.net;

import com.skyframework.islandcore.api.network.ActionOutcome;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.Optional;

// Generic reply for every action C2S packet introduced in Sprint "acciones de isla" (create,
// upgrade, delete request/confirm, settings, biome, invite/accept/trust/remove, teleport):
// reasonKey is one of the ActionReason constants, present only when success == false. The client
// is responsible for turning reasonKey into localized/styled text; this payload carries no
// human-readable message, matching how the server-side services themselves stopped formatting
// messages for the acting player in this same sprint.
public record ActionResultS2C(boolean success, Optional<String> reasonKey) implements CustomPayload {

	public static final CustomPayload.Id<ActionResultS2C> ID = new CustomPayload.Id<>(NetworkChannels.ACTION_RESULT_S2C);

	private static final PacketCodec<ByteBuf, Optional<String>> REASON_CODEC = PacketCodecs.optional(PacketCodecs.STRING);

	public static final PacketCodec<RegistryByteBuf, ActionResultS2C> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL, ActionResultS2C::success,
			REASON_CODEC, ActionResultS2C::reasonKey,
			ActionResultS2C::new
	);

	public static ActionResultS2C ok() {
		return new ActionResultS2C(true, Optional.empty());
	}

	public static ActionResultS2C fail(String reasonKey) {
		return new ActionResultS2C(false, Optional.of(reasonKey));
	}

	// Every action packet handler ends with ServerPlayNetworking.send(player,
	// ActionResultS2C.fromOutcome(outcome)) — this is the one place that bridges the server-only
	// ActionOutcome<T> (Paso 0) to the wire format (Paso 1).
	public static ActionResultS2C fromOutcome(ActionOutcome<?> outcome) {
		return outcome.success() ? ok() : fail(outcome.reason());
	}

	@Override
	public CustomPayload.Id<ActionResultS2C> getId() {
		return ID;
	}
}
