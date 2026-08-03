package com.skyframework.islandcore.net.handshake;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Server-side copy of IslandCoreClient's network.handshake.ServerHandshakeS2C — see the note on
// ClientHandshakeC2S about why this can't be a shared class between the two mod projects.
//
// Field order: protocolVersion, protocolCompatible, isOperator. protocolCompatible is new —
// true when the protocolVersion the client sent in ClientHandshakeC2S matched this server's
// NetworkChannels.PROTOCOL_VERSION, computed by the handler in ServerPacketHandlers, not blocking
// the handshake reply either way. It's placed right after protocolVersion since the two are about
// the same thing (this connection's protocol match); isOperator is unrelated and stays last.
public record ServerHandshakeS2C(int protocolVersion, boolean protocolCompatible, boolean isOperator) implements CustomPayload {
	public static final CustomPayload.Id<ServerHandshakeS2C> ID = new CustomPayload.Id<>(NetworkChannels.HANDSHAKE_S2C);

	public static final PacketCodec<RegistryByteBuf, ServerHandshakeS2C> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, ServerHandshakeS2C::protocolVersion,
			PacketCodecs.BOOL, ServerHandshakeS2C::protocolCompatible,
			PacketCodecs.BOOL, ServerHandshakeS2C::isOperator,
			ServerHandshakeS2C::new
	);

	@Override
	public CustomPayload.Id<ServerHandshakeS2C> getId() {
		return ID;
	}
}
