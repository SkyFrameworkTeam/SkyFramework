package com.skyframework.islandcore.net.party;

import com.mojang.authlib.GameProfile;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.party.model.PartyData;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Pure assembly, mirroring IslandSnapshotBuilder: every field comes straight from PartyData/
// PartyRegistry, no business logic lives here.
public final class PartyStatusBuilder {

	private PartyStatusBuilder() {
	}

	public static PartyStatusS2C build(MinecraftServer server, PartyData party) {
		List<PartyStatusS2C.MemberEntry> members = new ArrayList<>();
		for (UUID memberUuid : party.getMembers()) {
			members.add(new PartyStatusS2C.MemberEntry(memberUuid, resolveName(server, memberUuid)));
		}

		List<PartyStatusS2C.AlliedPartyEntry> alliedParties = new ArrayList<>();
		for (UUID alliedPartyId : party.getAlliedPartyIds()) {
			String alliedName = IslandCoreMod.PARTY_REGISTRY.getParty(alliedPartyId)
					.map(PartyData::getName)
					.orElse("");
			alliedParties.add(new PartyStatusS2C.AlliedPartyEntry(alliedPartyId, alliedName));
		}

		return new PartyStatusS2C(
				true,
				party.getPartyId(),
				party.getName(),
				party.getLeaderUuid(),
				resolveName(server, party.getLeaderUuid()),
				members,
				alliedParties
		);
	}

	private static String resolveName(MinecraftServer server, UUID playerUuid) {
		return server.getUserCache().getByUuid(playerUuid).map(GameProfile::getName).orElse(playerUuid.toString());
	}
}
