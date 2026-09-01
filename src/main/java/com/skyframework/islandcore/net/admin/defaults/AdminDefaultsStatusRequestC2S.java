package com.skyframework.islandcore.net.admin.defaults;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose, same shape as FlagsStatusRequestC2S — requests the current server-wide
// defaults (not any specific island's), see AdminDefaultsStatusS2C. Operator-only.
public record AdminDefaultsStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<AdminDefaultsStatusRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.ADMIN_DEFAULTS_STATUS_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, AdminDefaultsStatusRequestC2S> CODEC =
			PacketCodec.unit(new AdminDefaultsStatusRequestC2S());

	@Override
	public CustomPayload.Id<AdminDefaultsStatusRequestC2S> getId() {
		return ID;
	}
}
