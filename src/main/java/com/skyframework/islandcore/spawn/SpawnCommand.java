package com.skyframework.islandcore.spawn;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.util.ServerLang;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

// "/spawn", open to every player (no OP required).
public class SpawnCommand {

	private SpawnCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("spawn")
						.executes(SpawnCommand::executeSpawn))
		);
	}

	private static int executeSpawn(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		if (!IslandCoreMod.SPAWN_CONFIG.isEnabled()) {
			source.sendError(ServerLang.of(player,
					"El comando /spawn está desactivado en este servidor.", "The /spawn command is disabled on this server."));
			return 0;
		}

		// requestSpawn() sends its own feedback/error messages directly to the player (it may
		// need to message them again later, when the warmup finishes or is cancelled).
		IslandCoreMod.TELEPORT_MANAGER.requestSpawn(player);

		return 1;
	}
}
