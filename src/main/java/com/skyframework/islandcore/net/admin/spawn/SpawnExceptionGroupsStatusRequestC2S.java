package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

// Empty on purpose, like SpawnFlagsStatusRequestC2S. Replies with the SAME ExceptionGroupsStatusS2C
// a normal island's own ExceptionGroupsStatusRequestC2S replies with
// (FlagsStatusBuilder#buildExceptionGroupsStatus already takes an Island directly, no playerUuid at
// all, so it's fully reusable here).
public record SpawnExceptionGroupsStatusRequestC2S() implements CustomPayload {
	public static final CustomPayload.Id<SpawnExceptionGroupsStatusRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.SPAWN_EXCEPTION_GROUPS_STATUS_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, SpawnExceptionGroupsStatusRequestC2S> CODEC =
			PacketCodec.unit(new SpawnExceptionGroupsStatusRequestC2S());

	@Override
	public CustomPayload.Id<SpawnExceptionGroupsStatusRequestC2S> getId() {
		return ID;
	}
}
