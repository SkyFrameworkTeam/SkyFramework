package com.skyframework.islandcore.net.admin.defaults;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Sets the server-wide default preset for one exception group ("/island admin exceptions
// set-default <group> <preset>" network equivalent). Wire field order: groupId, preset
// ("nadie"/"miembros"/"aliados"/"todos"). Operator-only.
public record AdminExceptionSetServerDefaultC2S(String groupId, String preset) implements CustomPayload {
	public static final CustomPayload.Id<AdminExceptionSetServerDefaultC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.ADMIN_EXCEPTION_SET_SERVER_DEFAULT_C2S);

	public static final PacketCodec<RegistryByteBuf, AdminExceptionSetServerDefaultC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, AdminExceptionSetServerDefaultC2S::groupId,
			PacketCodecs.STRING, AdminExceptionSetServerDefaultC2S::preset,
			AdminExceptionSetServerDefaultC2S::new
	);

	@Override
	public CustomPayload.Id<AdminExceptionSetServerDefaultC2S> getId() {
		return ID;
	}
}
