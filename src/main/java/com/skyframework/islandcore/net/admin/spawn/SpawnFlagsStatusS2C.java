package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;
import com.skyframework.islandcore.net.flag.FlagsStatusS2C;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

// Same shape and same FlagsStatusS2C.FlagEntry as a normal island's own FlagsStatusS2C (built by
// the exact same FlagsStatusBuilder#buildFlagsStatus, just given the Spawn island) — a SEPARATE
// payload id purely so the client routes it to its own cache instead of colliding with
// SettingsScreen's (which reads FlagsStatusS2C for the ACTING player's own island). Wire field
// order: flags (list of FlagsStatusS2C.FlagEntry, same order/meaning as there).
public record SpawnFlagsStatusS2C(List<FlagsStatusS2C.FlagEntry> flags) implements CustomPayload {

	public static final CustomPayload.Id<SpawnFlagsStatusS2C> ID = new CustomPayload.Id<>(NetworkChannels.SPAWN_FLAGS_STATUS_S2C);

	private static final PacketCodec<RegistryByteBuf, List<FlagsStatusS2C.FlagEntry>> FLAG_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, FlagsStatusS2C.FlagEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, SpawnFlagsStatusS2C> CODEC = PacketCodec.tuple(
			FLAG_LIST_CODEC, SpawnFlagsStatusS2C::flags,
			SpawnFlagsStatusS2C::new
	);

	@Override
	public CustomPayload.Id<SpawnFlagsStatusS2C> getId() {
		return ID;
	}
}
