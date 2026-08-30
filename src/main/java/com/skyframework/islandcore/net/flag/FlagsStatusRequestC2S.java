package com.skyframework.islandcore.net.flag;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose: the server identifies the requester from the connection itself
// (context.player()) and looks up their own island, same as IslandSnapshotRequestC2S.
public record FlagsStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<FlagsStatusRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.FLAGS_STATUS_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, FlagsStatusRequestC2S> CODEC =
			PacketCodec.unit(new FlagsStatusRequestC2S());

	@Override
	public CustomPayload.Id<FlagsStatusRequestC2S> getId() {
		return ID;
	}
}
