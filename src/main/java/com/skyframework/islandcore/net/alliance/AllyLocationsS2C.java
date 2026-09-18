package com.skyframework.islandcore.net.alliance;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Pushed periodically by AllyLocationBroadcaster (not requested by the client) to every player with
// receivePositions=true, one entry per eligible allied-island player currently online, in the same
// dimension as the receiver, with sendPosition=true — see AllyLocationBroadcaster for the full
// eligibility rule. entries is empty (never omitted entirely) when the receiver has no eligible
// allies right now, so the client can clear any stale HUD indicator instead of leaving it frozen on
// a last-known position. No dimension field: every entry is already guaranteed to share the
// receiver's own current dimension.
public record AllyLocationsS2C(List<Entry> entries) implements CustomPayload {
	public static final CustomPayload.Id<AllyLocationsS2C> ID = new CustomPayload.Id<>(NetworkChannels.ALLY_LOCATIONS_S2C);

	private static final PacketCodec<RegistryByteBuf, List<Entry>> ENTRY_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, Entry.CODEC);

	public static final PacketCodec<RegistryByteBuf, AllyLocationsS2C> CODEC = PacketCodec.tuple(
			ENTRY_LIST_CODEC, AllyLocationsS2C::entries,
			AllyLocationsS2C::new
	);

	@Override
	public CustomPayload.Id<AllyLocationsS2C> getId() {
		return ID;
	}

	public record Entry(UUID uuid, String name, double x, double y, double z) {
		public static final PacketCodec<RegistryByteBuf, Entry> CODEC = PacketCodec.tuple(
				Uuids.PACKET_CODEC, Entry::uuid,
				PacketCodecs.STRING, Entry::name,
				PacketCodecs.DOUBLE, Entry::x,
				PacketCodecs.DOUBLE, Entry::y,
				PacketCodecs.DOUBLE, Entry::z,
				Entry::new
		);
	}
}
