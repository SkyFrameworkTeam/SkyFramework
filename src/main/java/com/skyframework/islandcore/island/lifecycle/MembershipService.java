package com.skyframework.islandcore.island.lifecycle;

import com.mojang.authlib.GameProfile;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.island.IslandPermission;
import com.skyframework.islandcore.api.network.ActionOutcome;
import com.skyframework.islandcore.api.network.ActionReason;
import com.skyframework.islandcore.island.model.IslandMember;
import com.skyframework.islandcore.island.model.IslandRole;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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
			targetPlayer.sendMessage(Text.literal(inviter.getGameProfile().getName()
					+ " te ha invitado a su isla. Usa /island accept en los próximos 5 minutos para unirte."), false);
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
			owner.sendMessage(Text.literal(
					player.getGameProfile().getName() + " ha aceptado tu invitación y se ha unido a tu isla."), false);
		}

		return ActionOutcome.ok(island);
	}

	public static ActionOutcome<Void> trust(ServerPlayerEntity executor, UUID targetUuid) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(executor.getUuid());
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		IslandMember member = new IslandMember(targetUuid, IslandRole.TRUSTED, Instant.now(), EnumSet.noneOf(IslandPermission.class));
		IslandCoreMod.ISLAND_REGISTRY.addMember(island.getIslandId(), member);

		return ActionOutcome.ok();
	}

	public static ActionOutcome<Void> untrust(ServerPlayerEntity executor, UUID targetUuid) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(executor.getUuid());
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		IslandCoreMod.ISLAND_REGISTRY.removeMember(island.getIslandId(), targetUuid);

		return ActionOutcome.ok();
	}

	// Same shape as trust()/untrust() above, but assigning/removing the ALLY role instead of
	// TRUSTED — used by "/island ally add/remove" (see IslandRole for how ALLY differs: same
	// per-flag defaults as VISITOR unless the owner opens a flag for it explicitly).
	public static ActionOutcome<Void> allyAdd(ServerPlayerEntity executor, UUID targetUuid) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(executor.getUuid());
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		IslandMember member = new IslandMember(targetUuid, IslandRole.ALLY, Instant.now(), EnumSet.noneOf(IslandPermission.class));
		IslandCoreMod.ISLAND_REGISTRY.addMember(island.getIslandId(), member);

		return ActionOutcome.ok();
	}

	public static ActionOutcome<Void> allyRemove(ServerPlayerEntity executor, UUID targetUuid) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(executor.getUuid());
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		IslandCoreMod.ISLAND_REGISTRY.removeMember(island.getIslandId(), targetUuid);

		return ActionOutcome.ok();
	}

	// Admin-scoped variants of trust()/untrust() above, for the Spawn admin block: they operate on
	// an explicit island rather than resolving it from the executor's own ownership, since the
	// operator running /island admin spawn trust/untrust never owns the Spawn island themselves.
	// Same upsert-safe addMember/removeMember underneath, so re-trusting or untrusting a
	// non-member is a harmless no-op, exactly like the player-facing versions.
	public static void trustOnIsland(Island island, UUID targetUuid) {
		IslandMember member = new IslandMember(targetUuid, IslandRole.TRUSTED, Instant.now(), EnumSet.noneOf(IslandPermission.class));
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
		if (role != IslandRole.MEMBER && role != IslandRole.TRUSTED) {
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
			targetPlayer.sendMessage(Text.literal(
					"Has sido expulsado de la isla de " + executor.getGameProfile().getName() + "."), false);
		}

		return ActionOutcome.ok();
	}

	// MemberRemoveC2S (network only — see net/island/MemberRemoveC2S) covers both /island untrust
	// and /island kick in one action, deciding which behavior applies from the target's CURRENT
	// role: TRUSTED -> untrust()'s behavior (demotion only, no eviction — losing trusted status
	// isn't grounds to force them out); MEMBER -> kick()'s behavior (full removal + eviction if
	// currently on the island). This is a new judgment call for the combined network action; the
	// two text commands are untouched and keep calling trust()/untrust()/kick() directly.
	public static ActionOutcome<Void> removeMember(ServerPlayerEntity executor, UUID targetUuid, MinecraftServer server) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(executor.getUuid());
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		IslandRole role = maybeIsland.get().getRoleOf(targetUuid);
		if (role == IslandRole.TRUSTED) {
			return untrust(executor, targetUuid);
		}
		return kick(executor, targetUuid, server);
	}
}
