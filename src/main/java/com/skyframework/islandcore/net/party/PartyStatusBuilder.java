package com.skyframework.islandcore.net.party;

import com.mojang.authlib.GameProfile;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.party.model.PartyData;

import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Pure assembly, mirroring IslandSnapshotBuilder: every field comes straight from PartyData/
// PartyRegistry/PartyInviteManager, no business logic lives here.
public final class PartyStatusBuilder {

	private PartyStatusBuilder() {
	}

	// incomingInvite is always Optional.empty() in practice for this branch: PartyInviteManager#
	// requestInvite refuses to invite a player who's already in a party, so a player who has one
	// (the only case that reaches this method) can never also have a pending invite. Still a real
	// parameter (not hardcoded) so the wire format doesn't silently lie if that invariant ever
	// changes — see PartyStatusS2C's own javadoc note.
	public static PartyStatusS2C build(MinecraftServer server, PartyData party) {
		List<PartyStatusS2C.MemberEntry> members = new ArrayList<>();
		for (UUID memberUuid : party.getMembers()) {
			members.add(new PartyStatusS2C.MemberEntry(memberUuid, resolveName(server, memberUuid)));
		}

		return new PartyStatusS2C(
				true,
				party.getPartyId(),
				party.getName(),
				party.getLeaderUuid(),
				resolveName(server, party.getLeaderUuid()),
				members,
				Optional.empty()
		);
	}

	// playerUuid is the requester — same reasoning as IslandSnapshotBuilder#build's own playerUuid
	// parameter: an incoming invite belongs to the player asking, not to the party being described.
	public static PartyStatusS2C buildAbsent(MinecraftServer server, UUID playerUuid) {
		return PartyStatusS2C.absent(buildIncomingInvite(server, playerUuid));
	}

	private static Optional<PartyStatusS2C.IncomingPartyInviteEntry> buildIncomingInvite(MinecraftServer server, UUID playerUuid) {
		return IslandCoreMod.PARTY_INVITE_MANAGER.getPendingInvite(playerUuid).map(invite -> {
			String partyName = IslandCoreMod.PARTY_REGISTRY.getParty(invite.partyId()).map(PartyData::getName).orElse("");
			int expiresInSeconds = (int) Math.max(0L, Duration.between(Instant.now(), invite.expiresAt()).getSeconds());
			return new PartyStatusS2C.IncomingPartyInviteEntry(resolveName(server, invite.invitedByUuid()), partyName, expiresInSeconds);
		});
	}

	private static String resolveName(MinecraftServer server, UUID playerUuid) {
		return server.getUserCache().getByUuid(playerUuid).map(GameProfile::getName).orElse(playerUuid.toString());
	}
}
