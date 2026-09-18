package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Full state of the receiving player's own party, or {@code hasParty = false} with default/empty
 * values in every other field if they don't have one — {@code hasParty = false} is a normal,
 * valid state (not an error), same as {@code IslandSnapshotS2C#exists}.
 *
 * <p>Field count (8) is past {@link PacketCodec#tuple}'s 6-argument limit, so {@link #CODEC} is
 * written by hand with {@link PacketCodec#of}, same as {@code IslandSnapshotS2C}.
 *
 * <p>Wire field order: {@code hasParty}, {@code partyId} (the zero UUID if {@code hasParty} is
 * false), {@code name} ({@code ""} if absent), {@code leaderUuid} (the zero UUID if absent),
 * {@code leaderName} (resolved via {@code server.getUserCache()}, {@code ""} if absent),
 * {@code members} (list of {@link MemberEntry} — every party member including the leader, same
 * set {@code PartyData#getMembers()} returns; empty if absent), {@code incomingInvite} (an invite
 * where the receiving player is the INVITEE, mirroring {@code IslandSnapshotS2C#incomingInvite};
 * empty if there is none pending, or it already expired. In practice only ever non-empty when
 * {@code hasParty} is false: {@code PartyInviteManager#requestInvite} refuses to invite a player
 * who's already in a party, so a player who currently has one can never also have a pending
 * invite).
 *
 * <p>No {@code alliedParties} field (retired along with {@code PartyData#alliedPartyIds} and
 * {@code /party ally add/remove} — see the "alianzas" consolidation sprint: individual-player
 * allies now live entirely on the island side, see {@code IslandSnapshotS2C}'s member list).
 */
public record PartyStatusS2C(
		boolean hasParty,
		UUID partyId,
		String name,
		UUID leaderUuid,
		String leaderName,
		List<MemberEntry> members,
		Optional<IncomingPartyInviteEntry> incomingInvite
) implements CustomPayload {

	public static final CustomPayload.Id<PartyStatusS2C> ID = new CustomPayload.Id<>(NetworkChannels.PARTY_STATUS_S2C);

	// Sentinel for "no party" — mirrors Island.SERVER_OWNER_UUID's own zero-UUID convention.
	public static final UUID NO_PARTY_UUID = new UUID(0, 0);

	private static final PacketCodec<RegistryByteBuf, List<MemberEntry>> MEMBER_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, MemberEntry.CODEC);
	private static final PacketCodec<RegistryByteBuf, Optional<IncomingPartyInviteEntry>> INCOMING_INVITE_CODEC =
			PacketCodecs.optional(IncomingPartyInviteEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, PartyStatusS2C> CODEC = PacketCodec.of(
			(value, buf) -> {
				PacketCodecs.BOOL.encode(buf, value.hasParty());
				Uuids.PACKET_CODEC.encode(buf, value.partyId());
				PacketCodecs.STRING.encode(buf, value.name());
				Uuids.PACKET_CODEC.encode(buf, value.leaderUuid());
				PacketCodecs.STRING.encode(buf, value.leaderName());
				MEMBER_LIST_CODEC.encode(buf, value.members());
				INCOMING_INVITE_CODEC.encode(buf, value.incomingInvite());
			},
			buf -> new PartyStatusS2C(
					PacketCodecs.BOOL.decode(buf),
					Uuids.PACKET_CODEC.decode(buf),
					PacketCodecs.STRING.decode(buf),
					Uuids.PACKET_CODEC.decode(buf),
					PacketCodecs.STRING.decode(buf),
					MEMBER_LIST_CODEC.decode(buf),
					INCOMING_INVITE_CODEC.decode(buf)
			)
	);

	// Mirrors SpawnBuildProtectionStatusS2C#absent(): the "no party" state, as a static factory on
	// the record itself. incomingInvite is still a real parameter here (not hardcoded empty) since
	// it's the one field that CAN be populated even while hasParty is false — see PartyStatusBuilder.
	public static PartyStatusS2C absent(Optional<IncomingPartyInviteEntry> incomingInvite) {
		return new PartyStatusS2C(false, NO_PARTY_UUID, "", NO_PARTY_UUID, "", List.of(), incomingInvite);
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

	// Wire field order: inviterName (resolved), partyName (resolved), expiresInSeconds.
	public record IncomingPartyInviteEntry(String inviterName, String partyName, int expiresInSeconds) {
		public static final PacketCodec<RegistryByteBuf, IncomingPartyInviteEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, IncomingPartyInviteEntry::inviterName,
				PacketCodecs.STRING, IncomingPartyInviteEntry::partyName,
				PacketCodecs.VAR_INT, IncomingPartyInviteEntry::expiresInSeconds,
				IncomingPartyInviteEntry::new
		);
	}
}
