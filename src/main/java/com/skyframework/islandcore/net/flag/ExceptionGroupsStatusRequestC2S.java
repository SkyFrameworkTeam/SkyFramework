package com.skyframework.islandcore.net.flag;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose, same as FlagsStatusRequestC2S.
public record ExceptionGroupsStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<ExceptionGroupsStatusRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.EXCEPTION_GROUPS_STATUS_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, ExceptionGroupsStatusRequestC2S> CODEC =
			PacketCodec.unit(new ExceptionGroupsStatusRequestC2S());

	@Override
	public CustomPayload.Id<ExceptionGroupsStatusRequestC2S> getId() {
		return ID;
	}
}
