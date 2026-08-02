package com.skyframework.islandcore.net.admin.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

// Network equivalent of "/island admin delete <player> confirm". Resolves the target's island via
// getIslandByOwner (same as AdminIslandDeleteC2S) then calls the same
// IslandDeletionService#confirmDeletion the text command uses.
public record AdminIslandDeleteConfirmC2S(UUID targetUuid) implements CustomPayload {

	public static final CustomPayload.Id<AdminIslandDeleteConfirmC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.ADMIN_ISLAND_DELETE_CONFIRM_C2S);

	public static final PacketCodec<RegistryByteBuf, AdminIslandDeleteConfirmC2S> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, AdminIslandDeleteConfirmC2S::targetUuid,
			AdminIslandDeleteConfirmC2S::new
	);

	@Override
	public CustomPayload.Id<AdminIslandDeleteConfirmC2S> getId() {
		return ID;
	}
}
