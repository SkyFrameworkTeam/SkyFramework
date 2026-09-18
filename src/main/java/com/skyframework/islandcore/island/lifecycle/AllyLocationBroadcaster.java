package com.skyframework.islandcore.island.lifecycle;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.island.model.IslandMember;
import com.skyframework.islandcore.island.model.IslandRole;
import com.skyframework.islandcore.net.alliance.AllyLocationsS2C;
import com.skyframework.islandcore.party.model.PartyData;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// Every BROADCAST_INTERVAL_TICKS (half a second), pushes each receiving player's eligible
// party+ally positions via AllyLocationsS2C — see that payload's javadoc for the general shape.
// Two independent candidate sets, each gated by its own send/receive toggle pair (see
// PlayerLocationSharingConfig):
//  - party: the receiver's own party members (party.getMembers(), leader included), each requiring
//    sendPositionToParty=true on their end and receivePositionsFromParty=true on the receiver's.
//    Works for a receiver with no island at all — a party has no island dependency.
//  - allies: players with IslandRole.ALLY on the receiver's OWN island (getIslandByOwner — a
//    non-owning member doesn't get this feed, only the owner), each requiring
//    sendPositionToAllies=true on their end and receivePositionsFromAllies=true on the receiver's.
// Both sets are merged (a player who is both a party member AND an island ally is only sent once)
// before the usual online/same-dimension filter applies identically to either origin.
public class AllyLocationBroadcaster {

	private static final int BROADCAST_INTERVAL_TICKS = 10;

	private int tickCounter;

	public void tickAll(MinecraftServer server) {
		tickCounter++;
		if (tickCounter % BROADCAST_INTERVAL_TICKS != 0) {
			return;
		}

		for (ServerPlayerEntity receiver : server.getPlayerManager().getPlayerList()) {
			boolean wantsParty = IslandCoreMod.LOCATION_SHARING_CONFIG.isReceivePositionsFromPartyEnabled(receiver.getUuid());
			boolean wantsAllies = IslandCoreMod.LOCATION_SHARING_CONFIG.isReceivePositionsFromAlliesEnabled(receiver.getUuid());
			if (!wantsParty && !wantsAllies) {
				continue;
			}

			ServerPlayNetworking.send(receiver, new AllyLocationsS2C(buildEntriesFor(receiver, server, wantsParty, wantsAllies)));
		}
	}

	// Empty (not omitted) when the receiver has no eligible party/ally candidates right now — see
	// AllyLocationsS2C's own javadoc for why that still gets sent.
	private List<AllyLocationsS2C.Entry> buildEntriesFor(
			ServerPlayerEntity receiver, MinecraftServer server, boolean wantsParty, boolean wantsAllies) {
		Set<UUID> candidates = new LinkedHashSet<>();

		if (wantsParty) {
			candidates.addAll(partyCandidates(receiver.getUuid()));
		}
		if (wantsAllies) {
			candidates.addAll(allyCandidates(receiver.getUuid()));
		}

		List<AllyLocationsS2C.Entry> entries = new ArrayList<>();
		for (UUID candidateUuid : candidates) {
			ServerPlayerEntity candidate = server.getPlayerManager().getPlayer(candidateUuid);
			if (candidate == null) {
				continue;
			}
			if (!candidate.getWorld().getRegistryKey().equals(receiver.getWorld().getRegistryKey())) {
				continue;
			}

			entries.add(new AllyLocationsS2C.Entry(
					candidateUuid, candidate.getGameProfile().getName(), candidate.getX(), candidate.getY(), candidate.getZ()));
		}

		return entries;
	}

	// The receiver's own party members (leader included, self excluded), each still requiring their
	// own sendPositionToParty=true — receivePositionsFromParty=true on the receiver only means they
	// WANT the feed, not that every party member has agreed to be seen on it.
	private static Set<UUID> partyCandidates(UUID receiverUuid) {
		Optional<PartyData> maybeParty = IslandCoreMod.PARTY_REGISTRY.getPartyOf(receiverUuid);
		if (maybeParty.isEmpty()) {
			return Set.of();
		}

		Set<UUID> candidates = new LinkedHashSet<>();
		for (UUID memberUuid : maybeParty.get().getMembers()) {
			if (!memberUuid.equals(receiverUuid) && IslandCoreMod.LOCATION_SHARING_CONFIG.isSendPositionToPartyEnabled(memberUuid)) {
				candidates.add(memberUuid);
			}
		}
		return candidates;
	}

	// Players with IslandRole.ALLY on the receiver's OWN island — see Island#getRoleOf and
	// MembershipService#allyAdd ("/island alliance add"). Only the owner gets this feed (matches
	// getIslandByOwner, not a broader "any member's island" lookup) — same scope the old
	// island-to-island alliance feed already had.
	private static Set<UUID> allyCandidates(UUID receiverUuid) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(receiverUuid);
		if (maybeIsland.isEmpty()) {
			return Set.of();
		}

		Set<UUID> candidates = new LinkedHashSet<>();
		for (IslandMember member : maybeIsland.get().getMembers()) {
			if (member.role() == IslandRole.ALLY && IslandCoreMod.LOCATION_SHARING_CONFIG.isSendPositionToAlliesEnabled(member.playerUuid())) {
				candidates.add(member.playerUuid());
			}
		}
		return candidates;
	}
}
