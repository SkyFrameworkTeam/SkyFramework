package com.skyframework.islandcore.command;

import com.mojang.authlib.GameProfile;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.party.lifecycle.PartyDisbandRequests;
import com.skyframework.islandcore.party.model.PartyData;
import com.skyframework.islandcore.util.ServerLang;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;
import java.util.UUID;

// The /party command tree: player-facing, no OP requirement. Reuses the same registry+storage
// shape as /dimension (see PartyRegistryImpl) rather than the IslandRegistry/MembershipService
// path used by /island, since parties are deliberately independent of island/.
public class PartyCommand {

	private PartyCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("party")
						.then(CommandManager.literal("create")
								.then(CommandManager.argument("name", StringArgumentType.word())
										.executes(PartyCommand::executeCreate)))
						.then(CommandManager.literal("invite")
								.then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
										.executes(PartyCommand::executeInvite)))
						.then(CommandManager.literal("accept")
								.executes(PartyCommand::executeAccept))
						.then(CommandManager.literal("leave")
								.executes(PartyCommand::executeLeave))
						.then(CommandManager.literal("kick")
								.then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
										.executes(PartyCommand::executeKick)))
						.then(CommandManager.literal("rename")
								.then(CommandManager.argument("name", StringArgumentType.word())
										.executes(PartyCommand::executeRename)))
						.then(CommandManager.literal("disband")
								.executes(PartyCommand::executeDisband)
								.then(CommandManager.literal("confirm")
										.executes(PartyCommand::executeDisbandConfirm)))
						.then(CommandManager.literal("info")
								.executes(PartyCommand::executeInfo))
				)
		);
	}

	private static int executeCreate(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		String name = StringArgumentType.getString(ctx, "name");

		PartyData party;
		try {
			party = IslandCoreMod.PARTY_REGISTRY.createParty(name, player.getUuid());
		} catch (IllegalStateException e) {
			source.sendError(Text.literal(e.getMessage()));
			return 0;
		}

		source.sendFeedback(() -> ServerLang.of(player, "Party \"" + party.getName() + "\" creada. Eres el líder.", "Party \"" + party.getName() + "\" created. You are the leader."), false);
		return 1;
	}

	private static int executeInvite(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		GameProfile targetProfile = GameProfileArgumentType.getProfileArgument(ctx, "player").iterator().next();

		Optional<PartyData> maybeParty = requireLeaderOf(source, player.getUuid());
		if (maybeParty.isEmpty()) {
			return 0;
		}
		PartyData party = maybeParty.get();

		try {
			IslandCoreMod.PARTY_INVITE_MANAGER.requestInvite(party.getPartyId(), player.getUuid(), targetProfile.getId());
		} catch (IllegalStateException e) {
			source.sendError(Text.literal(e.getMessage()));
			return 0;
		}

		ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(targetProfile.getId());
		if (targetPlayer != null) {
			targetPlayer.sendMessage(ServerLang.of(targetPlayer, player.getGameProfile().getName()
					+ " te ha invitado a su party \"" + party.getName()
					+ "\". Usa /party accept en los próximos 5 minutos para unirte.",
					player.getGameProfile().getName()
					+ " has invited you to their party \"" + party.getName()
					+ "\". Use /party accept within the next 5 minutes to join."), false);
			source.sendFeedback(() -> ServerLang.of(player, "Invitación enviada a " + targetProfile.getName() + ".", "Invitation sent to " + targetProfile.getName() + "."), false);
		} else {
			source.sendFeedback(() -> ServerLang.of(player,
					"Invitación registrada para " + targetProfile.getName()
							+ " (no está conectado ahora mismo, pero podrá aceptarla si entra en los próximos 5 minutos).",
					"Invitation registered for " + targetProfile.getName()
							+ " (they are not connected right now, but will be able to accept it if they join within the next 5 minutes)."), false);
		}

		return 1;
	}

	private static int executeAccept(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		Optional<PartyData> maybeParty;
		try {
			maybeParty = IslandCoreMod.PARTY_INVITE_MANAGER.acceptInvite(player.getUuid());
		} catch (IllegalStateException e) {
			source.sendError(Text.literal(e.getMessage()));
			return 0;
		}

		if (maybeParty.isEmpty()) {
			source.sendError(ServerLang.of(player, "No tienes ninguna invitación de party pendiente (o ha caducado).", "You do not have any pending party invitation (or it has expired)."));
			return 0;
		}
		PartyData party = maybeParty.get();

		source.sendFeedback(() -> ServerLang.of(player, "¡Te has unido a la party \"" + party.getName() + "\"!", "You have joined the party \"" + party.getName() + "\"!"), false);

		ServerPlayerEntity leader = source.getServer().getPlayerManager().getPlayer(party.getLeaderUuid());
		if (leader != null) {
			leader.sendMessage(ServerLang.of(leader,
					player.getGameProfile().getName() + " ha aceptado tu invitación y se ha unido a la party.",
					player.getGameProfile().getName() + " has accepted your invitation and joined the party."), false);
		}

		return 1;
	}

	private static int executeLeave(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		Optional<PartyData> maybeParty = IslandCoreMod.PARTY_REGISTRY.getPartyOf(player.getUuid());
		if (maybeParty.isEmpty()) {
			source.sendError(ServerLang.of(player, "No perteneces a ninguna party.", "You do not belong to any party."));
			return 0;
		}
		PartyData party = maybeParty.get();
		boolean wasLeader = party.getLeaderUuid().equals(player.getUuid());

		boolean disbanded = IslandCoreMod.PARTY_REGISTRY.leaveParty(player.getUuid());

		if (disbanded) {
			source.sendFeedback(() -> ServerLang.of(player, "Has salido de la party \"" + party.getName() + "\". Al ser el único miembro, se ha disuelto.", "You have left the party \"" + party.getName() + "\". Since you were the only member, it has been disbanded."), false);
		} else if (wasLeader) {
			Optional<PartyData> updated = IslandCoreMod.PARTY_REGISTRY.getParty(party.getPartyId());
			String newLeaderName = updated.map(p -> resolveName(source.getServer(), p.getLeaderUuid())).orElse("otro miembro");
			source.sendFeedback(() -> ServerLang.of(player,
					"Has salido de la party \"" + party.getName() + "\". El liderazgo ha pasado a " + newLeaderName + ".",
					"You have left the party \"" + party.getName() + "\". Leadership has passed to " + newLeaderName + "."), false);
		} else {
			source.sendFeedback(() -> ServerLang.of(player, "Has salido de la party \"" + party.getName() + "\".", "You have left the party \"" + party.getName() + "\"."), false);
		}

		return 1;
	}

	private static int executeKick(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		GameProfile targetProfile = GameProfileArgumentType.getProfileArgument(ctx, "player").iterator().next();

		Optional<PartyData> maybeParty = requireLeaderOf(source, player.getUuid());
		if (maybeParty.isEmpty()) {
			return 0;
		}
		PartyData party = maybeParty.get();

		if (targetProfile.getId().equals(player.getUuid())) {
			source.sendError(ServerLang.of(player, "No puedes expulsarte a ti mismo. Usa /party leave o /party disband.", "You cannot kick yourself. Use /party leave or /party disband."));
			return 0;
		}
		if (!party.getMembers().contains(targetProfile.getId())) {
			source.sendError(ServerLang.of(player, targetProfile.getName() + " no es miembro de tu party.", targetProfile.getName() + " is not a member of your party."));
			return 0;
		}

		IslandCoreMod.PARTY_REGISTRY.removeMember(party.getPartyId(), targetProfile.getId());

		ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(targetProfile.getId());
		if (targetPlayer != null) {
			targetPlayer.sendMessage(ServerLang.of(targetPlayer, "Has sido expulsado de la party \"" + party.getName() + "\".", "You have been kicked from the party \"" + party.getName() + "\"."), false);
		}

		source.sendFeedback(() -> ServerLang.of(player, targetProfile.getName() + " ha sido expulsado de la party.", targetProfile.getName() + " has been kicked from the party."), false);
		return 1;
	}

	private static int executeRename(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		String newName = StringArgumentType.getString(ctx, "name");

		Optional<PartyData> maybeParty = requireLeaderOf(source, player.getUuid());
		if (maybeParty.isEmpty()) {
			return 0;
		}
		PartyData party = maybeParty.get();

		try {
			IslandCoreMod.PARTY_REGISTRY.renameParty(party.getPartyId(), newName);
		} catch (IllegalStateException e) {
			source.sendError(Text.literal(e.getMessage()));
			return 0;
		}

		source.sendFeedback(() -> ServerLang.of(player, "Party renombrada a \"" + newName + "\".", "Party renamed to \"" + newName + "\"."), false);
		return 1;
	}

	private static int executeDisband(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		Optional<PartyData> maybeParty = requireLeaderOf(source, player.getUuid());
		if (maybeParty.isEmpty()) {
			return 0;
		}
		PartyData party = maybeParty.get();

		PartyDisbandRequests.request(party.getPartyId());
		source.sendFeedback(() -> ((net.minecraft.text.MutableText) ServerLang.of(player,
				"¿Seguro que quieres disolver la party \"" + party.getName() + "\"? Usa /party disband confirm en los próximos "
						+ PartyDisbandRequests.TIMEOUT.toSeconds() + " segundos para confirmar.",
				"Are you sure you want to disband the party \"" + party.getName() + "\"? Use /party disband confirm within the next "
						+ PartyDisbandRequests.TIMEOUT.toSeconds() + " seconds to confirm.")).formatted(Formatting.RED), false);
		return 1;
	}

	private static int executeDisbandConfirm(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		Optional<PartyData> maybeParty = requireLeaderOf(source, player.getUuid());
		if (maybeParty.isEmpty()) {
			return 0;
		}
		PartyData party = maybeParty.get();

		if (!PartyDisbandRequests.confirm(party.getPartyId())) {
			source.sendError(ServerLang.of(player, "No hay ninguna solicitud de disolución pendiente (o ha caducado). Usa /party disband primero.", "There is no pending disband request (or it has expired). Use /party disband first."));
			return 0;
		}

		MinecraftServer server = source.getServer();
		for (UUID memberUuid : party.getMembers()) {
			if (memberUuid.equals(player.getUuid())) {
				continue;
			}
			ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberUuid);
			if (member != null) {
				member.sendMessage(ServerLang.of(member, "La party \"" + party.getName() + "\" ha sido disuelta por su líder.", "The party \"" + party.getName() + "\" has been disbanded by its leader."), false);
			}
		}

		IslandCoreMod.PARTY_REGISTRY.disbandParty(party.getPartyId());
		source.sendFeedback(() -> ServerLang.of(player, "Party \"" + party.getName() + "\" disuelta.", "Party \"" + party.getName() + "\" disbanded."), false);
		return 1;
	}

	private static int executeInfo(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		Optional<PartyData> maybeParty = IslandCoreMod.PARTY_REGISTRY.getPartyOf(player.getUuid());
		if (maybeParty.isEmpty()) {
			source.sendError(ServerLang.of(player, "No perteneces a ninguna party.", "You do not belong to any party."));
			return 0;
		}
		PartyData party = maybeParty.get();
		MinecraftServer server = source.getServer();

		source.sendFeedback(() -> ((net.minecraft.text.MutableText) ServerLang.of(player, "=== Party: " + party.getName() + " ===", "=== Party: " + party.getName() + " ===")).formatted(Formatting.BOLD, Formatting.AQUA), false);
		source.sendFeedback(() -> ((net.minecraft.text.MutableText) ServerLang.of(player, "Líder: " + resolveName(server, party.getLeaderUuid()), "Leader: " + resolveName(server, party.getLeaderUuid()))).formatted(Formatting.GOLD), false);

		source.sendFeedback(() -> ((net.minecraft.text.MutableText) ServerLang.of(player, "Miembros (" + party.getMembers().size() + "):", "Members (" + party.getMembers().size() + "):")).formatted(Formatting.BOLD, Formatting.GOLD), false);
		for (UUID memberUuid : party.getMembers()) {
			String name = resolveName(server, memberUuid);
			boolean isLeader = memberUuid.equals(party.getLeaderUuid());
			source.sendFeedback(() -> ServerLang.of(player, "- " + name + (isLeader ? " (líder)" : ""), "- " + name + (isLeader ? " (leader)" : "")), false);
		}

		return 1;
	}

	// Shared guard for every leader-only subcommand: reports "no tienes party"/"no eres el líder"
	// and returns empty on failure, or the executor's party on success.
	private static Optional<PartyData> requireLeaderOf(ServerCommandSource source, UUID playerUuid) {
		Optional<PartyData> maybeParty = IslandCoreMod.PARTY_REGISTRY.getPartyOf(playerUuid);
		if (maybeParty.isEmpty()) {
			source.sendError(Text.literal("No perteneces a ninguna party."));
			return Optional.empty();
		}

		PartyData party = maybeParty.get();
		if (!party.getLeaderUuid().equals(playerUuid)) {
			source.sendError(Text.literal("Solo el líder de la party puede hacer esto."));
			return Optional.empty();
		}

		return Optional.of(party);
	}

	private static String resolveName(MinecraftServer server, UUID playerUuid) {
		return server.getUserCache().getByUuid(playerUuid).map(GameProfile::getName).orElse(playerUuid.toString());
	}
}
