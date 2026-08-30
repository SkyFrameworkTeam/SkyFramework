package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Full state of the receiving player's own party, or {@code hasParty = false} with default/empty
 * values in every other field if they don't have one — {@code hasParty = false} is a normal,
 * valid state (not an error), same as {@code IslandSnapshotS2C#exists}.
 *
 * <p>Field count (7) is past {@link PacketCodec#tuple}'s 6-argument limit, so {@link #CODEC} is
 * written by hand with {@link PacketCodec#of}, same as {@code IslandSnapshotS2C}.
 *
 * <p>Wire field order: {@code hasParty}, {@code partyId} (the zero UUID if {@code hasParty} is
 * false), {@code name} ({@code ""} if absent), {@code leaderUuid} (the zero UUID if absent),
 * {@code leaderName} (resolved via {@code server.getUserCache()}, {@code ""} if absent),
 * {@code members} (list of {@link MemberEntry} — every party member including the leader, same
 * set {@code PartyData#getMembers()} returns; empty if absent), {@code alliedParties} (list of
 * {@link AlliedPartyEntry} — the parties THIS party has declared allies, unidirectional; empty if
 * absent).
 */
public record PartyStatusS2C(
		boolean hasParty,
		UUID partyId,
		String name,
		UUID leaderUuid,
		String leaderName,
		List<MemberEntry> members,
		List<AlliedPartyEntry> alliedParties
) implements CustomPayload {

	public static final CustomPayload.Id<PartyStatusS2C> ID = new CustomPayload.Id<>(NetworkChannels.PARTY_STATUS_S2C);

	// Sentinel for "no party" — mirrors Island.SERVER_OWNER_UUID's own zero-UUID convention.
	public static final UUID NO_PARTY_UUID = new UUID(0, 0);

	private static final PacketCodec<RegistryByteBuf, List<MemberEntry>> MEMBER_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, MemberEntry.CODEC);
	private static final PacketCodec<RegistryByteBuf, List<AlliedPartyEntry>> ALLIED_PARTY_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, AlliedPartyEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, PartyStatusS2C> CODEC = PacketCodec.of(
			(value, buf) -> {
				PacketCodecs.BOOL.encode(buf, value.hasParty());
				Uuids.PACKET_CODEC.encode(buf, value.partyId());
				PacketCodecs.STRING.encode(buf, value.name());
				Uuids.PACKET_CODEC.encode(buf, value.leaderUuid());
				PacketCodecs.STRING.encode(buf, value.leaderName());
				MEMBER_LIST_CODEC.encode(buf, value.members());
				ALLIED_PARTY_LIST_CODEC.encode(buf, value.alliedParties());
			},
			buf -> new PartyStatusS2C(
					PacketCodecs.BOOL.decode(buf),
					Uuids.PACKET_CODEC.decode(buf),
					PacketCodecs.STRING.decode(buf),
					Uuids.PACKET_CODEC.decode(buf),
					PacketCodecs.STRING.decode(buf),
					MEMBER_LIST_CODEC.decode(buf),
					ALLIED_PARTY_LIST_CODEC.decode(buf)
			)
	);

	// Mirrors SpawnBuildProtectionStatusS2C#absent(): the "no party" state, as a static factory on
	// the record itself.
	public static PartyStatusS2C absent() {
		return new PartyStatusS2C(false, NO_PARTY_UUID, "", NO_PARTY_UUID, "", List.of(), List.of());
	}

	@Override
	public CustomPayload.Id<PartyStatusS2C> getId() {
		return ID;
	}

	public record MemberEntry(UUID uuid, String name) {
		public static final PacketCodec<RegistryByteBuf, MemberEntry> CODEC = PacketCodec.tuple(
				Uuids.PACKET_CODEC, MemberEntry::uuid,
				PacketCodecs.STRING, MemberEntry::name,
				MemberEntry::new
		);
	}

	public record AlliedPartyEntry(UUID partyId, String name) {
		public static final PacketCodec<RegistryByteBuf, AlliedPartyEntry> CODEC = PacketCodec.tuple(
				Uuids.PACKET_CODEC, AlliedPartyEntry::partyId,
				PacketCodecs.STRING, AlliedPartyEntry::name,
				AlliedPartyEntry::new
		);
	}
}
