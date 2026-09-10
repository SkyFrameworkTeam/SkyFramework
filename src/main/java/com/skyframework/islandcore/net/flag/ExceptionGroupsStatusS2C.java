package com.skyframework.islandcore.net.flag;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Sent only when the requesting player has an island — if not, the server sends
 * {@code ActionResultS2C.fail(ActionReason.NO_ISLAND)} instead, no empty/placeholder snapshot.
 * Wire field order: {@code groups} (list of {@link GroupEntry}, in
 * {@code ExceptionGroupRegistry.getAllGroups()}'s registration order).
 *
 * <p>Exception groups now resolve per role exactly like ROLE_BASED flags (see ExceptionResolver) —
 * this record mirrors {@link FlagsStatusS2C.FlagEntry}'s shape instead of the old single
 * {@code enabled} boolean.
 */
public record ExceptionGroupsStatusS2C(List<GroupEntry> groups) implements CustomPayload {

	public static final CustomPayload.Id<ExceptionGroupsStatusS2C> ID =
			new CustomPayload.Id<>(NetworkChannels.EXCEPTION_GROUPS_STATUS_S2C);

	private static final PacketCodec<RegistryByteBuf, List<GroupEntry>> GROUP_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, GroupEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, ExceptionGroupsStatusS2C> CODEC = PacketCodec.tuple(
			GROUP_LIST_CODEC, ExceptionGroupsStatusS2C::groups,
			ExceptionGroupsStatusS2C::new
	);

	@Override
	public CustomPayload.Id<ExceptionGroupsStatusS2C> getId() {
		return ID;
	}

	// Wire field order: groupId, category ("BLOCK" or "ENTITY", ExceptionGroupCategory#name()),
	// resolvedByRole (one FlagsStatusS2C.RoleValueEntry per IslandRole, reusing that record exactly
	// — same shape, no reason to duplicate it), currentPreset ("nadie"/"miembros"/"aliados"/"todos"
	// if the current VISITOR/ALLY/MEMBER combination exactly matches one of those presets,
	// or "custom" if not — see ExceptionResolver#currentPreset), ownerConfigurable
	// (ExceptionGroup#isOwnerConfigurable()).
	public record GroupEntry(
			String groupId, String category, List<FlagsStatusS2C.RoleValueEntry> resolvedByRole, String currentPreset, boolean ownerConfigurable
	) {
		private static final PacketCodec<RegistryByteBuf, List<FlagsStatusS2C.RoleValueEntry>> ROLE_VALUE_LIST_CODEC =
				PacketCodecs.collection(ArrayList::new, FlagsStatusS2C.RoleValueEntry.CODEC);

		public static final PacketCodec<RegistryByteBuf, GroupEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, GroupEntry::groupId,
				PacketCodecs.STRING, GroupEntry::category,
				ROLE_VALUE_LIST_CODEC, GroupEntry::resolvedByRole,
				PacketCodecs.STRING, GroupEntry::currentPreset,
				PacketCodecs.BOOL, GroupEntry::ownerConfigurable,
				GroupEntry::new
		);
	}
}
