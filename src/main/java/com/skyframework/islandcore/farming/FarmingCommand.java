package com.skyframework.islandcore.farming;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.skyframework.islandcore.IslandCoreMod;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

// "/farming", open to every player (no OP required).
public class FarmingCommand {

	private FarmingCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("farming")
						.executes(FarmingCommand::executeFarming))
		);
	}

	private static int executeFarming(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		if (!IslandCoreMod.FARMING_CONFIG.isEnabled()) {
			source.sendError(Text.literal("El comando /farming está desactivado en este servidor."));
			return 0;
		}

		// requestFarming() sends its own feedback/error messages directly to the player (it may
		// need to message them again later, when the warmup finishes or is cancelled).
		IslandCoreMod.TELEPORT_MANAGER.requestFarming(player);

		return 1;
	}
}
