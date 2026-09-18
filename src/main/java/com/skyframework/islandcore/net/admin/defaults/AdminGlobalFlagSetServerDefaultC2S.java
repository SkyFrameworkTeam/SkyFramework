package com.skyframework.islandcore.net.admin.defaults;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Sets the server-wide default TriState override for one ISLAND_GLOBAL flag ("/island admin flags
// set-default <flag> allow|deny|default" network equivalent — see AdminDefaultsStatusS2C's class
// javadoc, this is the piece that used to be command-only). Wire field order: flagId, value
// ("allow"/"deny"/"default", same TriState#name().toLowerCase() vocabulary FlagSetC2S already uses
// for an island's own override). Operator-only; ROLE_BASED flags aren't reachable through this
// payload — see AdminFlagSetServerDefaultC2S for those.
public record AdminGlobalFlagSetServerDefaultC2S(String flagId, String value) implements CustomPayload {
	public static final CustomPayload.Id<AdminGlobalFlagSetServerDefaultC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.ADMIN_GLOBAL_FLAG_SET_SERVER_DEFAULT_C2S);

	public static final PacketCodec<RegistryByteBuf, AdminGlobalFlagSetServerDefaultC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, AdminGlobalFlagSetServerDefaultC2S::flagId,
			PacketCodecs.STRING, AdminGlobalFlagSetServerDefaultC2S::value,
			AdminGlobalFlagSetServerDefaultC2S::new
	);

	@Override
	public CustomPayload.Id<AdminGlobalFlagSetServerDefaultC2S> getId() {
		return ID;
	}
}
