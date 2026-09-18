package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;
import com.skyframework.islandcore.net.flag.ExceptionGroupsStatusS2C;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

// Same shape and same ExceptionGroupsStatusS2C.GroupEntry as a normal island's own
// ExceptionGroupsStatusS2C (built by the exact same FlagsStatusBuilder#buildExceptionGroupsStatus,
// just given the Spawn island) — a SEPARATE payload id purely so the client routes it to its own
// cache instead of colliding with SettingsScreen's. Wire field order: groups (list of
// ExceptionGroupsStatusS2C.GroupEntry, same order/meaning as there).
public record SpawnExceptionGroupsStatusS2C(List<ExceptionGroupsStatusS2C.GroupEntry> groups) implements CustomPayload {

	public static final CustomPayload.Id<SpawnExceptionGroupsStatusS2C> ID =
			new CustomPayload.Id<>(NetworkChannels.SPAWN_EXCEPTION_GROUPS_STATUS_S2C);

	private static final PacketCodec<RegistryByteBuf, List<ExceptionGroupsStatusS2C.GroupEntry>> GROUP_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, ExceptionGroupsStatusS2C.GroupEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, SpawnExceptionGroupsStatusS2C> CODEC = PacketCodec.tuple(
			GROUP_LIST_CODEC, SpawnExceptionGroupsStatusS2C::groups,
			SpawnExceptionGroupsStatusS2C::new
	);

	@Override
	public CustomPayload.Id<SpawnExceptionGroupsStatusS2C> getId() {
		return ID;
	}
}
