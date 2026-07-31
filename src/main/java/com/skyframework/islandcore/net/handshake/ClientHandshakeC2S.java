package com.skyframework.islandcore.net.handshake;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Server-side copy of IslandCoreClient's network.handshake.ClientHandshakeC2S. Fabric's
// networking has no shared-payload mechanism across separate mod jars, so both sides
// independently define the same channel id and wire format; they must be kept in sync by hand.
public record ClientHandshakeC2S(int protocolVersion) implements CustomPayload {
	public static final CustomPayload.Id<ClientHandshakeC2S> ID = new CustomPayload.Id<>(NetworkChannels.HANDSHAKE_C2S);

	public static final PacketCodec<RegistryByteBuf, ClientHandshakeC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, ClientHandshakeC2S::protocolVersion,
			ClientHandshakeC2S::new
	);

	@Override
	public CustomPayload.Id<ClientHandshakeC2S> getId() {
		return ID;
	}
}
