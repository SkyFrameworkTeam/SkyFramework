package com.skyframework.islandcore.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Countdown tick for a two-step confirmation window (currently only /island delete's 30-second
// confirm window; IslandDeletionService#getPendingDeletionRemainingSeconds was added in this same
// sprint specifically to back this payload). Defined and registered so a future sprint's
// broadcasting loop (and IslandCoreClient's Delete Island screen) has a stable wire format to
// target, but no periodic sender is wired up yet in this sprint — the request/confirm actions
// themselves already reply immediately via ActionResultS2C, and that's sufficient for a chat/GUI
// confirmation prompt without a live countdown. Building the tick broadcaster itself is left for
// when a screen actually needs to render a moving countdown, to avoid adding an unused tick loop.
public record PendingConfirmationTickS2C(int remainingSeconds) implements CustomPayload {

	public static final CustomPayload.Id<PendingConfirmationTickS2C> ID =
			new CustomPayload.Id<>(NetworkChannels.PENDING_CONFIRMATION_TICK_S2C);

	public static final PacketCodec<RegistryByteBuf, PendingConfirmationTickS2C> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, PendingConfirmationTickS2C::remainingSeconds,
			PendingConfirmationTickS2C::new
	);

	@Override
	public CustomPayload.Id<PendingConfirmationTickS2C> getId() {
		return ID;
	}
}
