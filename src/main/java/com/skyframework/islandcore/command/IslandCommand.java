package com.skyframework.islandcore.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.permission.IslandPermissions;
import com.skyframework.islandcore.island.generation.BasicPlatformGenerator;
import com.skyframework.islandcore.island.model.IslandSetting;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.command.CommandSource;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

// First real /island command tree, distinct from the temporary /ic and /islandcore debug tree.
// Other subcommands (trust, ...) still live only under debug and will be
// migrated here one at a time in future sprints.
public class IslandCommand {

	private static final RegistryKey<World> ISLANDS_DIMENSION =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

	private static final BasicPlatformGenerator PLATFORM_GENERATOR = new BasicPlatformGenerator();

	private static final List<String> SETTING_NAMES = List.of("firespread", "pvp", "mobdamage");

	private IslandCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("island")
						.then(CommandManager.literal("create")
								.executes(IslandCommand::executeCreate))
						.then(CommandManager.literal("info")
								.executes(IslandCommand::executeInfo))
						.then(CommandManager.literal("list")
								.executes(IslandCommand::executeList))
						.then(CommandManager.literal("upgrade")
								.executes(IslandCommand::executeUpgrade))
						.then(CommandManager.literal("home")
								.executes(IslandCommand::executeHome))
						.then(CommandManager.literal("sethome")
								.executes(IslandCommand::executeSetHome))
						.then(CommandManager.literal("settings")
								.then(CommandManager.argument("setting", StringArgumentType.word())
										.suggests((ctx, builder) -> CommandSource.suggestMatching(SETTING_NAMES, builder))
										.then(CommandManager.argument("value", BoolArgumentType.bool())
												.executes(IslandCommand::executeSettings))))
						.then(CommandManager.literal("delete")
								.executes(IslandCommand::executeDelete)
								.then(CommandManager.literal("confirm")
										.executes(IslandCommand::executeDeleteConfirm)))
				)
		);
	}

	private static int executeCreate(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		if (IslandCoreMod.PERMISSION_PROVIDER.hasPermission(player.getUuid(), IslandPermissions.ISLAND_CREATE_DENY)) {
			source.sendError(Text.literal("No tienes permitido crear una isla en este servidor."));
			return 0;
		}

		Island island;
		try {
			island = IslandCoreMod.ISLAND_REGISTRY.createIsland(player.getUuid(), ISLANDS_DIMENSION);
		} catch (IllegalStateException e) {
			source.sendError(Text.literal("Ya tienes una isla. Usa /island info para ver sus datos."));
			return 0;
		}

		BlockPos center = island.getCenter();

		ServerWorld islandsWorld = source.getServer().getWorld(ISLANDS_DIMENSION);
		PLATFORM_GENERATOR.generate(islandsWorld, center, island.getIslandSize());

		source.sendFeedback(() -> Text.literal("¡Isla creada! Centro: ("
				+ center.getX() + ", " + center.getY() + ", " + center.getZ() + ")"), false);

		return 1;
	}

	private static int executeInfo(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid());
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("You don't own an island."));
			return 0;
		}

		IslandMessages.sendIslandDetails(source, maybeIsland.get());
		return 1;
	}

	private static int executeList(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid());
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("No tienes ninguna isla todavía. Usa /island create para crear una."));
			return 0;
		}

		IslandMessages.sendIslandDetails(source, maybeIsland.get());
		return 1;
	}

	private static int executeUpgrade(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid());
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("No tienes ninguna isla todavía."));
			return 0;
		}

		Island island = maybeIsland.get();
		int maxSize = IslandCoreMod.PERMISSION_PROVIDER.getHighestSizeAllowed(player.getUuid());
		int currentSize = island.getIslandSize();

		if (maxSize <= currentSize) {
			source.sendFeedback(() -> Text.literal(
					"Ya tienes el tamaño máximo permitido por tus permisos actuales (tamaño actual: " + currentSize + ")."), false);
			return 1;
		}

		try {
			IslandCoreMod.ISLAND_REGISTRY.resizeIsland(island.getIslandId(), maxSize);
		} catch (IllegalArgumentException e) {
			// maxSize exceeds the island's reserved plot: a permissions/plot misconfiguration,
			// not something the player can fix themselves.
			source.sendError(Text.literal(
					"El tamaño permitido por tus permisos excede la parcela reservada de tu isla. Contacta con un administrador del servidor."));
			return 0;
		}

		source.sendFeedback(() -> Text.literal("¡Isla ampliada! Nuevo tamaño: " + maxSize + "."), false);

		return 1;
	}

	private static int executeHome(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		// requestHome() sends its own feedback/error messages directly to the player
		// (it may need to message them again later, when the warmup finishes or is cancelled).
		IslandCoreMod.TELEPORT_MANAGER.requestHome(player);

		return 1;
	}

	private static int executeSetHome(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid());
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("No tienes ninguna isla todavía."));
			return 0;
		}

		Island island = maybeIsland.get();
		boolean inIslandsDimension = player.getWorld().getRegistryKey().equals(ISLANDS_DIMENSION);
		boolean withinBuiltIsland = island.getBounds().contains(player.getBlockPos());

		if (!inIslandsDimension || !withinBuiltIsland) {
			source.sendError(Text.literal("El home debe fijarse dentro de la parte ya construida de tu isla."));
			return 0;
		}

		IslandCoreMod.ISLAND_REGISTRY.updateHomeLocation(island.getIslandId(), player.getBlockPos());

		source.sendFeedback(() -> Text.literal("Home actualizado a tu posición actual."), false);

		return 1;
	}

	private static int executeSettings(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		String settingName = StringArgumentType.getString(ctx, "setting");
		boolean value = BoolArgumentType.getBool(ctx, "value");

		IslandSetting setting = resolveSetting(settingName);
		if (setting == null) {
			source.sendError(Text.literal("Ajuste desconocido. Ajustes disponibles: firespread, pvp, mobdamage."));
			return 0;
		}

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid());
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("No tienes ninguna isla todavía."));
			return 0;
		}

		Island island = maybeIsland.get();
		// getIslandByOwner already only returns islands this player literally owns, so this is
		// currently redundant, but kept explicit per spec in case that lookup ever changes
		// (e.g. members getting their own island-lookup path in a future sprint).
		if (!island.getOwnerUuid().equals(player.getUuid())) {
			source.sendError(Text.literal("Solo el propietario de la isla puede cambiar sus ajustes."));
			return 0;
		}

		IslandCoreMod.ISLAND_REGISTRY.updateIslandSetting(island.getIslandId(), setting, value);

		source.sendFeedback(() -> Text.literal(settingName.toLowerCase() + " = " + value), false);

		return 1;
	}

	private static int executeDelete(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid());
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("No tienes ninguna isla todavía."));
			return 0;
		}

		try {
			IslandCoreMod.DELETION_SERVICE.requestDeletion(maybeIsland.get().getIslandId(), player.getUuid());
		} catch (IllegalArgumentException | IllegalStateException e) {
			source.sendError(Text.literal(e.getMessage()));
			return 0;
		}

		source.sendFeedback(() -> Text.literal(
				"¿Seguro que quieres borrar tu isla? Esta acción no se puede deshacer. "
						+ "Usa /island delete confirm en los próximos 30 segundos para confirmar."), false);

		return 1;
	}

	private static int executeDeleteConfirm(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid());
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("No tienes ninguna isla todavía."));
			return 0;
		}

		boolean confirmed = IslandCoreMod.DELETION_SERVICE.confirmDeletion(maybeIsland.get().getIslandId(), player.getUuid());
		if (!confirmed) {
			source.sendError(Text.literal(
					"No hay ninguna solicitud de borrado pendiente (o ha expirado). Usa /island delete primero."));
			return 0;
		}

		source.sendFeedback(() -> Text.literal("Tu isla se está borrando..."), false);

		return 1;
	}

	private static IslandSetting resolveSetting(String name) {
		if (name.equalsIgnoreCase("firespread")) {
			return IslandSetting.FIRE_SPREAD;
		}
		if (name.equalsIgnoreCase("pvp")) {
			return IslandSetting.PVP_DAMAGE;
		}
		if (name.equalsIgnoreCase("mobdamage")) {
			return IslandSetting.MOB_DAMAGE;
		}
		return null;
	}
}
