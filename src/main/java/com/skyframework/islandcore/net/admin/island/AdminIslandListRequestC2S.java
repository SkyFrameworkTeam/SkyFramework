package com.skyframework.islandcore.net.admin.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Requests one page of the admin island list. Wire field order: {@code page} (int, 0-indexed —
 * page 0 is the first page), {@code pageSize} (int, clamped to at least 1 server-side),
 * {@code searchQuery} (String, empty meaning "no filter" — filtered against the resolved owner
 * name, not the raw UUID). See {@link AdminIslandListS2C} for the reply.
 */
public record AdminIslandListRequestC2S(int page, int pageSize, String searchQuery) implements CustomPayload {

	public static final CustomPayload.Id<AdminIslandListRequestC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.ADMIN_ISLAND_LIST_REQUEST_C2S);

	public static final PacketCodec<RegistryByteBuf, AdminIslandListRequestC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, AdminIslandListRequestC2S::page,
			PacketCodecs.VAR_INT, AdminIslandListRequestC2S::pageSize,
			PacketCodecs.STRING, AdminIslandListRequestC2S::searchQuery,
			AdminIslandListRequestC2S::new
	);

	@Override
	public CustomPayload.Id<AdminIslandListRequestC2S> getId() {
		return ID;
	}
}
