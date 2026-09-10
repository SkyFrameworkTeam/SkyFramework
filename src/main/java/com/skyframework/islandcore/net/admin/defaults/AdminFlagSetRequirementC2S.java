package com.skyframework.islandcore.net.admin.defaults;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Sets (or clears) the LuckPerms node required to change one flag ("/island admin flags require
// <flag> <nodo|ninguno>" network equivalent) — see FlagPermissionRequirements's class javadoc.
// Wire field order: flagId, permissionNode. An empty string for permissionNode clears the
// requirement (mirrors "ninguno" on the text-command path); any other value sets/replaces it.
// Operator-only. Unlike AdminFlagSetServerDefaultC2S, this reaches both ROLE_BASED and
// ISLAND_GLOBAL flags, since the requirement gate applies to any flag a player can change.
public record AdminFlagSetRequirementC2S(String flagId, String permissionNode) implements CustomPayload {
	public static final CustomPayload.Id<AdminFlagSetRequirementC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.ADMIN_FLAG_SET_REQUIREMENT_C2S);

	public static final PacketCodec<RegistryByteBuf, AdminFlagSetRequirementC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, AdminFlagSetRequirementC2S::flagId,
			PacketCodecs.STRING, AdminFlagSetRequirementC2S::permissionNode,
			AdminFlagSetRequirementC2S::new
	);

	@Override
	public CustomPayload.Id<AdminFlagSetRequirementC2S> getId() {
		return ID;
	}
}
