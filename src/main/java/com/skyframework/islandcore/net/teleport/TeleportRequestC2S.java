package com.skyframework.islandcore.net.teleport;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// One packet for all four teleport actions rather than four empty payloads, since they share the
// exact same "no extra data, dispatch to TeleportManager" shape. type dispatches to
// TeleportManager#requestHome/requestSpawn/requestRtp/requestFarming respectively; the SPAWN/
// FARMING branches additionally replicate the enabled-check SpawnCommand/FarmingCommand already
// do before calling TeleportManager (see ActionReason#SPAWN_DISABLED/FARMING_DISABLED), since
// that check currently lives in the text command, not in TeleportManagerImpl itself.
//
// No progress/countdown packet accompanies this one for HOME/SPAWN/FARMING's warmup: those three
// already message the player directly (in chat) at the start of the warmup, once per second during
// it, and on completion/cancellation — TeleportManagerImpl's existing tickAll() loop. Adding a
// structured progress S2C is left for a future sprint if a GUI screen actually needs to render a
// numeric countdown independent of chat text; this immediate ActionResultS2C reply is enough to
// tell the client the warmup started (or why it didn't).
public record TeleportRequestC2S(Type type) implements CustomPayload {

	public enum Type {
		HOME, SPAWN, RTP, FARMING
	}

	public static final CustomPayload.Id<TeleportRequestC2S> ID = new CustomPayload.Id<>(NetworkChannels.TELEPORT_REQUEST_C2S);

	private static final PacketCodec<ByteBuf, Type> TYPE_CODEC = PacketCodecs.STRING.xmap(Type::valueOf, Enum::name);

	public static final PacketCodec<RegistryByteBuf, TeleportRequestC2S> CODEC = PacketCodec.tuple(
			TYPE_CODEC, TeleportRequestC2S::type,
			TeleportRequestC2S::new
	);

	@Override
	public CustomPayload.Id<TeleportRequestC2S> getId() {
		return ID;
	}
}
