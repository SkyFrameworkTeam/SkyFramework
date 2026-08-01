package com.skyframework.islandcore.net.teleport;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose, like IslandSnapshotRequestC2S: the server acts on context.player(), never on
// client-supplied data.
public record TeleportStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<TeleportStatusRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.TELEPORT_STATUS_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, TeleportStatusRequestC2S> CODEC =
			PacketCodec.unit(new TeleportStatusRequestC2S());

	@Override
	public CustomPayload.Id<TeleportStatusRequestC2S> getId() {
		return ID;
	}
}
