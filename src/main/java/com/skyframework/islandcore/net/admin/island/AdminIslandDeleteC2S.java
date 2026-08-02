package com.skyframework.islandcore.net.admin.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

// Network equivalent of "/island admin delete <player>": resolves the target's island via
// getIslandByOwner, then calls the exact same IslandDeletionService#requestDeletion the text
// command uses. Reply is a plain ActionResultS2C — no dedicated confirmation-prompt payload,
// same as the text command's own chat message being the only feedback today.
public record AdminIslandDeleteC2S(UUID targetUuid) implements CustomPayload {

	public static final CustomPayload.Id<AdminIslandDeleteC2S> ID = new CustomPayload.Id<>(NetworkChannels.ADMIN_ISLAND_DELETE_C2S);

	public static final PacketCodec<RegistryByteBuf, AdminIslandDeleteC2S> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, AdminIslandDeleteC2S::targetUuid,
			AdminIslandDeleteC2S::new
	);

	@Override
	public CustomPayload.Id<AdminIslandDeleteC2S> getId() {
		return ID;
	}
}
