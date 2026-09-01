package com.skyframework.islandcore.net.admin.defaults;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Sets the server-wide default preset for one ROLE_BASED flag ("/island admin flags set-default
// <flag> <preset>" network equivalent). Wire field order: flagId, preset ("nadie"/"miembros"/
// "aliados"/"todos"). Operator-only; ISLAND_GLOBAL flags aren't reachable through this payload (see
// AdminDefaultsStatusS2C's class javadoc).
public record AdminFlagSetServerDefaultC2S(String flagId, String preset) implements CustomPayload {
	public static final CustomPayload.Id<AdminFlagSetServerDefaultC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.ADMIN_FLAG_SET_SERVER_DEFAULT_C2S);

	public static final PacketCodec<RegistryByteBuf, AdminFlagSetServerDefaultC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, AdminFlagSetServerDefaultC2S::flagId,
			PacketCodecs.STRING, AdminFlagSetServerDefaultC2S::preset,
			AdminFlagSetServerDefaultC2S::new
	);

	@Override
	public CustomPayload.Id<AdminFlagSetServerDefaultC2S> getId() {
		return ID;
	}
}
