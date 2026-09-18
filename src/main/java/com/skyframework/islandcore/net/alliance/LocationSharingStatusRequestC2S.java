package com.skyframework.islandcore.net.alliance;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose: the server identifies the requester from the connection itself
// (context.player()), same as AllianceStatusRequestC2S.
public record LocationSharingStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<LocationSharingStatusRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.LOCATION_SHARING_STATUS_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, LocationSharingStatusRequestC2S> CODEC =
			PacketCodec.unit(new LocationSharingStatusRequestC2S());

	@Override
	public CustomPayload.Id<LocationSharingStatusRequestC2S> getId() {
		return ID;
	}
}
