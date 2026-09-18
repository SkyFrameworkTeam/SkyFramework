package com.skyframework.islandcore.island.lifecycle;

import com.mojang.authlib.GameProfile;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.island.IslandPermission;
import com.skyframework.islandcore.api.network.ActionOutcome;
import com.skyframework.islandcore.api.network.ActionReason;
import com.skyframework.islandcore.island.model.IslandMember;
import com.skyframework.islandcore.island.model.IslandRole;
import com.skyframework.islandcore.util.ServerLang;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Extracted from IslandCommand (Sprint "acciones de isla"): membership actions
 * (invite/accept/trust/untrust/kick). Like IslandActionService, no method here messages the
 * *acting* player — but unlike it, some methods here DO message a *third party* directly
 * (the invitee, the kicked player): that's a real side effect that must happen regardless of
 * whether the actor came from a command or a network packet, not presentation formatting for
 * the actor's own reply.
 */
public final class MembershipService {

	private MembershipService() {
	}

	// data = whether the target was online at invite time (Boolean), so the caller can pick
	// between the two existing "invitation sent"/"invitation registered" messages.
	public static ActionOutcome<Boolean> invite(ServerPlayerEntity inviter, UUID targetUuid, MinecraftServer server) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(inviter.getUuid());
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}
		Island island = maybeIsland.get();

		ActionOutcome<Void> inviteOutcome = IslandCoreMod.INVITE_MANAGER.invite(island.getIslandId(), inviter.getUuid(), targetUuid);
		if (!inviteOutcome.success()) {
			return ActionOutcome.fail(inviteOutcome.reason());
		}

		ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetUuid);
		if (targetPlayer != null) {
			targetPlayer.sendMessage(ServerLang.of(targetPlayer,
					inviter.getGameProfile().getName() + " te ha invitado a su isla. Usa /island accept en los próximos 5 minutos para unirte.",
					inviter.getGameProfile().getName() + " has invited you to their island. Use /island accept within the next 5 minutes to join."), false);
		}

		return ActionOutcome.ok(targetPlayer != null);
	}

	// Network-only entry point for MemberInviteC2S, which has no Brigadier GameProfileArgumentType
	// to resolve a name to a UUID for free like the text command does (targetProfile.getId()).
	// Checks online players by exact name first (instant, no disk/network I/O), then falls back to
	// the server's offline profile cache — the same fallback GameProfileArgumentType itself relies
	// on for names that aren't currently online.
	public static ActionOutcome<Boolean> inviteByName(ServerPlayerEntity inviter, String targetName, MinecraftServer server) {
		Optional<UUID> targetUuid = resolveUuid(targetName, server);
		if (targetUuid.isEmpty()) {
			return ActionOutcome.fail(ActionReason.TARGET_NOT_FOUND);
		}

		return invite(inviter, targetUuid.get(), server);
	}

	// Exposed for other network-only admin paths (SpawnAuthorizedPlayerAddC2S) that, like
	// inviteByName above, have no Brigadier GameProfileArgumentType to resolve a name for free.
	public static Optional<UUID> resolvePlayerUuid(String name, MinecraftServer server) {
		return resolveUuid(name, server);
	}

	private static Optional<UUID> resolveUuid(String name, MinecraftServer server) {
		ServerPlayerEntity online = server.getPlayerManager().getPlayer(name);
		if (online != null) {
			return Optional.of(online.getUuid());
		}

		return server.getUserCache().findByName(name).map(GameProfile::getId);
	}

	public static ActionOutcome<Island> acceptInvite(ServerPlayerEntity player, MinecraftServer server) {
		Optional<Island> maybeIsland = IslandCoreMod.INVITE_MANAGER.acceptInvite(player.getUuid());
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_PENDING_INVITE);
		}

		Island island = maybeIsland.get();
		ServerPlayerEntity owner = server.getPlayerManager().getPlayer(island.getOwnerUuid());
		if (owner != null) {
			owner.sendMessage(ServerLang.of(owner,
					player.getGameProfile().getName() + " ha aceptado tu invitación y se ha unido a tu isla.",
					player.getGameProfile().getName() + " has accepted your invitation and joined your island."), false);
		}

		return ActionOutcome.ok(island);
	}

	// Unlike acceptInvite, there's no membership change to roll back — just consumes the pending
	// invite server-side so "Ignorar" on the client is permanent instead of the banner reappearing
	// on the next snapshot refresh (the invite used to only be hidden locally, with the server-side
	// entry still alive until its own 5-minute timeout).
	public static ActionOutcome<Void> declineInvite(ServerPlayerEntity player, MinecraftServer server) {
		Optional<Island> maybeIsland = IslandCoreMod.INVITE_MANAGER.declineInvite(player.getUuid());
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_PENDING_INVITE);
		}

		Island island = maybeIsland.get();
		ServerPlayerEntity owner = server.getPlayerManager().getPlayer(island.getOwnerUuid());
		if (owner != null) {
			owner.sendMessage(ServerLang.of(owner,
					player.getGameProfile().getName() + " ha rechazado tu invitación.",
					player.getGameProfile().getName() + " has declined your invitation."), false);
		}

		return ActionOutcome.ok();
	}

	// Promotes to CO_OWNER unconditionally: whether targetUuid was already a plain MEMBER or not a
	// member at all, they end up CO_OWNER — addMember's upsert-by-playerUuid handles both cases the
	// same way, no invitation/acceptance needed (matches trust's historical no-invite behavior).
	// CO_OWNER is a hard-coded always-ALLOW role (see IslandRole's javadoc) — this is the only way
	// into it.
	public static ActionOutcome<Void> trust(ServerPlayerEntity executor, UUID targetUuid) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(executor.getUuid());
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		IslandMember member = new IslandMember(targetUuid, IslandRole.CO_OWNER, Instant.now(), EnumSet.noneOf(IslandPermission.class));
		IslandCoreMod.ISLAND_REGISTRY.addMember(island.getIslandId(), member);

		return ActionOutcome.ok();
	}

	// Demotes an existing CO_OWNER back to plain MEMBER — does NOT remove them from the island (use
	// kick()/removeMember() for that). Fails with NOT_CO_OWNER if the target isn't currently
	// CO_OWNER: untrust only ever applies to someone who actually holds that status, there's
	// nothing to "un-trust" about a plain MEMBER.
	public static ActionOutcome<Void> untrust(ServerPlayerEntity executor, UUID targetUuid) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(executor.getUuid());
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		if (island.getRoleOf(targetUuid) != IslandRole.CO_OWNER) {
			return ActionOutcome.fail(ActionReason.NOT_CO_OWNER);
		}

		IslandMember member = new IslandMember(targetUuid, IslandRole.MEMBER, Instant.now(), EnumSet.noneOf(IslandPermission.class));
		IslandCoreMod.ISLAND_REGISTRY.addMember(island.getIslandId(), member);

		return ActionOutcome.ok();
	}

	// The network entry point for MembersScreen's "Trust" toggle button (MemberTrustC2S): promotes
	// a MEMBER to CO_OWNER or demotes a CO_OWNER back to MEMBER depending on the target's CURRENT
	// role, so one button/one packet covers both directions. Fails with NOT_A_MEMBER for anyone who
	// is neither (ALLY/VISITOR) — the client only ever shows this button on MEMBER/CO_OWNER rows,
	// so that's a defensive guard, not an expected path.
	public static ActionOutcome<Void> toggleCoOwner(ServerPlayerEntity executor, UUID targetUuid) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(executor.getUuid());
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		IslandRole role = maybeIsland.get().getRoleOf(targetUuid);
		if (role == IslandRole.CO_OWNER) {
			return untrust(executor, targetUuid);
		}
		if (role == IslandRole.MEMBER) {
			return trust(executor, targetUuid);
		}
		return ActionOutcome.fail(ActionReason.NOT_A_MEMBER);
	}

	// Same shape as trust()/untrust() above, but assigning/removing the ALLY role instead of
	// CO_OWNER — used by "/island alliance add/remove" and MemberAllyAddC2S/RemoveC2S (see
	// IslandRole for how ALLY differs: same per-flag defaults as VISITOR unless the owner opens a
	// flag for it explicitly). Unlike trust/untrust/kick/invite (OWNER-only, via getIslandByOwner),
	// this one also allows CO_OWNER — see resolveManagedIsland — per the "alianzas" consolidation
	// sprint's explicit requirement that a co-owner can manage the ally list too.
	public static ActionOutcome<Void> allyAdd(ServerPlayerEntity executor, UUID targetUuid) {
		Optional<Island> maybeIsland = resolveManagedIsland(executor.getUuid());
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}
		if (targetUuid.equals(executor.getUuid())) {
			return ActionOutcome.fail(ActionReason.ALLIANCE_SELF);
		}

		Island island = maybeIsland.get();
		IslandMember member = new IslandMember(targetUuid, IslandRole.ALLY, Instant.now(), EnumSet.noneOf(IslandPermission.class));
		IslandCoreMod.ISLAND_REGISTRY.addMember(island.getIslandId(), member);

		return ActionOutcome.ok();
	}

	public static ActionOutcome<Void> allyRemove(ServerPlayerEntity executor, UUID targetUuid) {
		Optional<Island> maybeIsland = resolveManagedIsland(executor.getUuid());
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		IslandCoreMod.ISLAND_REGISTRY.removeMember(island.getIslandId(), targetUuid);

		return ActionOutcome.ok();
	}

	// OWNER's own island if they have one, otherwise the first island (there can only ever be one)
	// where they hold CO_OWNER — there's no direct player->island index for non-owners in
	// IslandRegistryApi, so this falls back to scanning getAllIslands(), acceptable here since
	// alliance management is a rare, non-hot-path action (unlike, say, per-tick protection checks).
	// Public: IslandCommand#executeAllianceList also needs it, to list the same island's allies
	// allyAdd/allyRemove would manage.
	public static Optional<Island> resolveManagedIsland(UUID playerUuid) {
		Optional<Island> owned = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(playerUuid);
		if (owned.isPresent()) {
			return owned;
		}
		return IslandCoreMod.ISLAND_REGISTRY.getAllIslands().stream()
				.filter(island -> island.getRoleOf(playerUuid) == IslandRole.CO_OWNER)
				.findFirst();
	}

	// Admin-scoped variants of trust()/untrust() above, for the Spawn admin block: they operate on
	// an explicit island rather than resolving it from the executor's own ownership, since the
	// operator running /island admin spawn trust/untrust never owns the Spawn island themselves.
	// Unlike the player-facing untrust() (which only demotes CO_OWNER -> MEMBER, see above),
	// untrustOnIsland keeps its original simpler full-removal semantics: Spawn's "authorized to
	// build" list has no separate MEMBER tier of its own in practice, so there's nothing useful to
	// demote into — same upsert-safe addMember/removeMember underneath, so re-trusting or
	// untrusting a non-member is a harmless no-op either way.
	public static void trustOnIsland(Island island, UUID targetUuid) {
		IslandMember member = new IslandMember(targetUuid, IslandRole.CO_OWNER, Instant.now(), EnumSet.noneOf(IslandPermission.class));
		IslandCoreMod.ISLAND_REGISTRY.addMember(island.getIslandId(), member);
	}

	public static void untrustOnIsland(Island island, UUID targetUuid) {
		IslandCoreMod.ISLAND_REGISTRY.removeMember(island.getIslandId(), targetUuid);
	}

	public static ActionOutcome<Void> kick(ServerPlayerEntity executor, UUID targetUuid, MinecraftServer server) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(executor.getUuid());
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		IslandRole role = island.getRoleOf(targetUuid);
		if (role != IslandRole.MEMBER && role != IslandRole.CO_OWNER) {
			return ActionOutcome.fail(ActionReason.NOT_A_MEMBER);
		}

		IslandCoreMod.ISLAND_REGISTRY.removeMember(island.getIslandId(), targetUuid);

		ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetUuid);
		if (targetPlayer != null
				&& targetPlayer.getWorld().getRegistryKey().equals(island.getDimension())
				&& island.getBounds().contains(targetPlayer.getBlockPos())) {
			EvictionTargetResolver.resolve(server)
					.ifPresent(target -> new com.skyframework.islandcore.teleport.VanillaTeleportBackend()
							.teleport(targetPlayer, target.world(), target.pos()));
		}

		if (targetPlayer != null) {
			targetPlayer.sendMessage(ServerLang.of(targetPlayer,
					"Has sido expulsado de la isla de " + executor.getGameProfile().getName() + ".",
					"You've been kicked from " + executor.getGameProfile().getName() + "'s island."), false);
		}

		return ActionOutcome.ok();
	}

	// MemberRemoveC2S (network only — see net/island/MemberRemoveC2S): full expulsion regardless of
	// the target's role (MEMBER or CO_OWNER) — matches kick()'s behavior exactly, now that
	// promoting/demoting between MEMBER and CO_OWNER has its own dedicated button/packet
	// (MemberTrustC2S -> toggleCoOwner above). Previously this branched on role and only demoted a
	// CO_OWNER (then TRUSTED) instead of expelling them; that responsibility moved to toggleCoOwner,
	// so this is now a thin pass-through kept only because the client still addresses it by this
	// packet name.
	public static ActionOutcome<Void> removeMember(ServerPlayerEntity executor, UUID targetUuid, MinecraftServer server) {
		return kick(executor, targetUuid, server);
	}
}
