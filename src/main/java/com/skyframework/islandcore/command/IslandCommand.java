package com.skyframework.islandcore.command;

import com.mojang.authlib.GameProfile;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.network.ActionOutcome;
import com.skyframework.islandcore.api.network.ActionReason;
import com.skyframework.islandcore.api.permission.IslandPermissions;
import com.skyframework.islandcore.island.lifecycle.IslandActionService;
import com.skyframework.islandcore.island.lifecycle.MembershipService;
import com.skyframework.islandcore.island.model.IslandSetting;
import com.skyframework.islandcore.teleport.SafeLandingChecker;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// The one and only /island command tree. The old /ic and /islandcore debug tree
// (command.debug.IslandDebugCommand) has been fully retired as of Sprint 12.
public class IslandCommand {

	private static final RegistryKey<World> ISLANDS_DIMENSION =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

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
						.then(CommandManager.literal("limits")
								.executes(IslandCommand::executeLimits))
						.then(CommandManager.literal("invite")
								.then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
										.executes(IslandCommand::executeInvite)))
						.then(CommandManager.literal("accept")
								.executes(IslandCommand::executeAccept))
						.then(CommandManager.literal("trust")
								.then(CommandManager.argument("player", EntityArgumentType.player())
										.executes(IslandCommand::executeTrust)))
						.then(CommandManager.literal("untrust")
								.then(CommandManager.argument("player", EntityArgumentType.player())
										.executes(IslandCommand::executeUntrust)))
						.then(CommandManager.literal("kick")
								.then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
										.executes(IslandCommand::executeKick)))
						.then(CommandManager.literal("biome")
								.then(CommandManager.argument("biome", IdentifierArgumentType.identifier())
										.suggests(IslandCommand::suggestBiomes)
										.executes(IslandCommand::executeBiome)))
						.then(CommandManager.literal("admin")
								.requires(source -> source.hasPermissionLevel(2))
								.then(CommandManager.literal("spawn")
										.then(CommandManager.literal("create")
												.then(CommandManager.argument("size", IntegerArgumentType.integer(1))
														.executes(IslandCommand::executeAdminSpawnCreate)))
										.then(CommandManager.literal("resize")
												.then(CommandManager.argument("size", IntegerArgumentType.integer(1))
														.executes(IslandCommand::executeAdminSpawnResize)))
										.then(CommandManager.literal("sethome")
												.executes(IslandCommand::executeAdminSpawnSetHome))
										.then(CommandManager.literal("settings")
												.then(CommandManager.literal("buildprotection")
														.then(CommandManager.argument("value", BoolArgumentType.bool())
																.executes(IslandCommand::executeAdminSpawnBuildProtection))))
										.then(CommandManager.literal("trust")
												.then(CommandManager.argument("player", EntityArgumentType.player())
														.executes(IslandCommand::executeAdminSpawnTrust)))
										.then(CommandManager.literal("untrust")
												.then(CommandManager.argument("player", EntityArgumentType.player())
														.executes(IslandCommand::executeAdminSpawnUntrust))))
								.then(CommandManager.literal("list")
										.executes(IslandCommand::executeAdminListAll)
										.then(CommandManager.argument("player", EntityArgumentType.player())
												.executes(IslandCommand::executeAdminListPlayer)))
								.then(CommandManager.literal("delete")
										.then(CommandManager.argument("player", EntityArgumentType.player())
												.executes(IslandCommand::executeAdminDelete)
												.then(CommandManager.literal("confirm")
														.executes(IslandCommand::executeAdminDeleteConfirm)))))
				)
		);
	}

	private static int executeCreate(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		ActionOutcome<Island> outcome = IslandActionService.create(player, source.getServer());
		if (!outcome.success()) {
			if (ActionReason.NO_PERMISSION.equals(outcome.reason())) {
				source.sendError(Text.literal("No tienes permitido crear una isla en este servidor."));
			} else {
				source.sendError(Text.literal("Ya tienes una isla. Usa /island info para ver sus datos."));
			}
			return 0;
		}

		BlockPos center = outcome.data().getCenter();
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

		IslandMessages.sendIslandSummaryPlayer(source, maybeIsland.get());
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

		IslandMessages.sendIslandSummaryPlayer(source, maybeIsland.get());
		return 1;
	}

	private static int executeUpgrade(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		ActionOutcome<IslandActionService.UpgradeResult> outcome = IslandActionService.upgrade(player.getUuid());
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendError(Text.literal("No tienes ninguna isla todavía."));
			} else {
				// maxSize exceeds the island's reserved plot: a permissions/plot misconfiguration,
				// not something the player can fix themselves.
				source.sendError(Text.literal(
						"El tamaño permitido por tus permisos excede la parcela reservada de tu isla. Contacta con un administrador del servidor."));
			}
			return 0;
		}

		IslandActionService.UpgradeResult result = outcome.data();
		if (result.oldSize() == result.newSize()) {
			int currentSize = result.oldSize();
			source.sendFeedback(() -> Text.literal(
					"Ya tienes el tamaño máximo permitido por tus permisos actuales (tamaño actual: " + currentSize + ")."), false);
			return 1;
		}

		int newSize = result.newSize();
		source.sendFeedback(() -> Text.literal("¡Isla ampliada! Nuevo tamaño: " + newSize + "."), false);

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

		if (!SafeLandingChecker.isSafe(player.getServerWorld(), player.getBlockPos())) {
			source.sendError(Text.literal(
					"No puedes fijar el home aquí, no hay suelo seguro debajo. Colócate sobre un bloque sólido."));
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

		Optional<IslandSetting> maybeSetting = IslandSetting.fromId(settingName);
		if (maybeSetting.isEmpty()) {
			source.sendError(Text.literal("Ajuste desconocido. Ajustes disponibles: firespread, pvp, mobdamage."));
			return 0;
		}

		ActionOutcome<Void> outcome = IslandActionService.updateSetting(player.getUuid(), maybeSetting.get(), value);
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendError(Text.literal("No tienes ninguna isla todavía."));
			} else {
				source.sendError(Text.literal("Solo el propietario de la isla puede cambiar sus ajustes."));
			}
			return 0;
		}

		source.sendFeedback(() -> Text.literal(settingName.toLowerCase() + " = " + value), false);

		return 1;
	}

	private static int executeDelete(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		ActionOutcome<Void> outcome = IslandActionService.requestDelete(player.getUuid());
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendError(Text.literal("No tienes ninguna isla todavía."));
			} else {
				source.sendError(Text.literal("Esta isla ya está en proceso de eliminación."));
			}
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

		ActionOutcome<Void> outcome = IslandActionService.confirmDelete(player.getUuid());
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendError(Text.literal("No tienes ninguna isla todavía."));
			} else {
				source.sendError(Text.literal(
						"No hay ninguna solicitud de borrado pendiente (o ha expirado). Usa /island delete primero."));
			}
			return 0;
		}

		source.sendFeedback(() -> Text.literal("Tu isla se está borrando..."), false);

		return 1;
	}

	private static int executeLimits(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		UUID playerUuid = player.getUuid();

		int maxSize = IslandCoreMod.PERMISSION_PROVIDER.getHighestSizeAllowed(playerUuid);
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(playerUuid);

		String sizeLine;
		if (maybeIsland.isPresent()) {
			int currentSize = maybeIsland.get().getIslandSize();
			sizeLine = currentSize >= maxSize
					? "Ya tienes el tamaño máximo permitido (" + maxSize + ")."
					: "Tamaño máximo permitido: " + maxSize + " (tu isla actual: " + currentSize
							+ " — puedes ejecutar /island upgrade).";
		} else {
			sizeLine = "Tamaño máximo permitido: " + maxSize + ".";
		}

		String cooldownLine = IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, IslandPermissions.TELEPORT_COOLDOWN_BYPASS)
				? "No tienes ningún cooldown para /island home."
				: "Cooldown de /island home: " + IslandCoreMod.PERMISSION_PROVIDER.getHomeCooldownSeconds(playerUuid) + " segundos.";

		String message = sizeLine + "\n" + cooldownLine;
		source.sendFeedback(() -> Text.literal(message), false);

		return 1;
	}

	private static int executeInvite(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		GameProfile targetProfile = GameProfileArgumentType.getProfileArgument(ctx, "player").iterator().next();

		ActionOutcome<Boolean> outcome = MembershipService.invite(player, targetProfile.getId(), source.getServer());
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendError(Text.literal("No tienes ninguna isla todavía."));
			} else if (ActionReason.ALREADY_OWNER.equals(outcome.reason())) {
				source.sendError(Text.literal("Ya eres el propietario de esta isla."));
			} else {
				source.sendError(Text.literal("Ese jugador ya es miembro de tu isla."));
			}
			return 0;
		}

		boolean targetOnline = outcome.data();
		if (targetOnline) {
			source.sendFeedback(() -> Text.literal("Invitación enviada a " + targetProfile.getName() + "."), false);
		} else {
			source.sendFeedback(() -> Text.literal(
					"Invitación registrada para " + targetProfile.getName()
							+ " (no está conectado ahora mismo, pero podrá aceptarla si entra en los próximos 5 minutos)."), false);
		}

		return 1;
	}

	private static int executeAccept(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		ActionOutcome<Island> outcome = MembershipService.acceptInvite(player, source.getServer());
		if (!outcome.success()) {
			source.sendError(Text.literal("No tienes ninguna invitación pendiente (o ha caducado)."));
			return 0;
		}

		source.sendFeedback(() -> Text.literal("¡Te has unido a la isla!"), false);

		return 1;
	}

	private static int executeTrust(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity executor = source.getPlayerOrThrow();
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

		ActionOutcome<Void> outcome = MembershipService.trust(executor, target.getUuid());
		if (!outcome.success()) {
			source.sendError(Text.literal("No tienes una isla."));
			return 0;
		}

		String targetName = target.getGameProfile().getName();
		source.sendFeedback(() -> Text.literal(targetName + " ahora es de confianza en tu isla."), false);

		return 1;
	}

	private static int executeUntrust(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity executor = source.getPlayerOrThrow();
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

		ActionOutcome<Void> outcome = MembershipService.untrust(executor, target.getUuid());
		if (!outcome.success()) {
			source.sendError(Text.literal("No tienes una isla."));
			return 0;
		}

		String targetName = target.getGameProfile().getName();
		source.sendFeedback(() -> Text.literal(targetName + " ya no tiene acceso especial a tu isla."), false);

		return 1;
	}

	private static int executeKick(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		GameProfile targetProfile = GameProfileArgumentType.getProfileArgument(ctx, "player").iterator().next();
		UUID targetUuid = targetProfile.getId();

		ActionOutcome<Void> outcome = MembershipService.kick(player, targetUuid, source.getServer());
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendError(Text.literal("No tienes ninguna isla todavía."));
			} else {
				source.sendError(Text.literal(targetProfile.getName() + " no es miembro de tu isla."));
			}
			return 0;
		}

		source.sendFeedback(() -> Text.literal(targetProfile.getName() + " ha sido expulsado de tu isla."), false);

		return 1;
	}

	private static CompletableFuture<Suggestions> suggestBiomes(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
		ServerPlayerEntity player = ctx.getSource().getPlayer();
		if (player == null) {
			return builder.buildFuture();
		}

		Collection<Identifier> availableBiomes =
				IslandCoreMod.BIOME_TIER_REGISTRY.getAvailableBiomes(player.getUuid(), IslandCoreMod.PERMISSION_PROVIDER);
		return CommandSource.suggestIdentifiers(availableBiomes, builder);
	}

	@SuppressWarnings("unchecked")
	private static int executeBiome(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		Identifier biomeId = IdentifierArgumentType.getIdentifier(ctx, "biome");

		ActionOutcome<Object> outcome = IslandActionService.changeBiome(player, biomeId, source.getServer());
		if (!outcome.success()) {
			switch (outcome.reason()) {
				case ActionReason.NO_ISLAND -> source.sendError(Text.literal("No tienes ninguna isla todavía."));
				case ActionReason.NOT_OWNER ->
						source.sendError(Text.literal("Solo el propietario de la isla puede cambiar su bioma."));
				case ActionReason.BIOME_NOT_FOUND -> source.sendError(Text.literal("El bioma " + biomeId + " no existe."));
				case ActionReason.BIOME_LOCKED -> {
					List<String> tierIds = (List<String>) outcome.data();
					if (tierIds == null || tierIds.isEmpty()) {
						source.sendError(Text.literal("El bioma " + biomeId + " no está disponible."));
					} else {
						source.sendError(Text.literal("No tienes acceso al bioma " + biomeId
								+ ". Se desbloquea con el/los tier(s): " + String.join(", ", tierIds) + "."));
					}
				}
				case ActionReason.COOLDOWN_ACTIVE -> {
					long remainingSeconds = (Long) outcome.data();
					String remaining = formatDuration(Duration.ofSeconds(remainingSeconds));
					source.sendError(Text.literal(
							"Todavía no puedes volver a cambiar el bioma de tu isla. Podrás hacerlo en " + remaining + "."));
				}
				case ActionReason.DIMENSION_UNAVAILABLE ->
						source.sendError(Text.literal("La dimensión de tu isla no está disponible ahora mismo."));
				default -> throw new IllegalStateException("Unhandled ActionReason from changeBiome: " + outcome.reason());
			}
			return 0;
		}

		source.sendFeedback(() -> Text.literal(
				"Cambiando el bioma de tu isla a " + biomeId + "... puede tardar unos segundos en islas grandes."), false);

		return 1;
	}

	private static String formatDuration(Duration duration) {
		long totalSeconds = Math.max(1, duration.getSeconds());
		long days = totalSeconds / 86400;
		long hours = (totalSeconds % 86400) / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;

		if (days > 0) {
			return days + (days == 1 ? " día " : " días ") + hours + (hours == 1 ? " hora" : " horas");
		}
		if (hours > 0) {
			return hours + (hours == 1 ? " hora " : " horas ") + minutes + (minutes == 1 ? " minuto" : " minutos");
		}
		if (minutes > 0) {
			return minutes + (minutes == 1 ? " minuto " : " minutos ") + seconds + (seconds == 1 ? " segundo" : " segundos");
		}
		return seconds + (seconds == 1 ? " segundo" : " segundos");
	}

	private static int executeAdminListAll(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource source = ctx.getSource();

		Collection<Island> islands = IslandCoreMod.ISLAND_REGISTRY.getAllIslands();
		if (islands.isEmpty()) {
			source.sendFeedback(() -> Text.literal("No islands exist yet."), false);
			return 0;
		}

		source.sendFeedback(() -> Text.literal("Islands (" + islands.size() + "):"), false);
		for (Island island : islands) {
			IslandMessages.sendIslandSummary(source, island);
		}

		return islands.size();
	}

	private static int executeAdminListPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(target.getUuid());
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal(target.getGameProfile().getName() + " no tiene ninguna isla."));
			return 0;
		}

		IslandMessages.sendIslandSummaryAdmin(source, maybeIsland.get());
		return 1;
	}

	private static int executeAdminSpawnCreate(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource source = ctx.getSource();
		int size = IntegerArgumentType.getInteger(ctx, "size");

		Island island;
		try {
			island = IslandCoreMod.ISLAND_REGISTRY.createSpawnIsland(ISLANDS_DIMENSION, size);
		} catch (IllegalStateException e) {
			source.sendError(Text.literal("The spawn island already exists."));
			return 0;
		}

		BlockPos center = island.getCenter();
		source.sendFeedback(() -> Text.literal("Created spawn island " + island.getIslandId()
				+ " at (" + center.getX() + ", " + center.getY() + ", " + center.getZ() + ")"), false);

		return 1;
	}

	private static int executeAdminSpawnResize(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource source = ctx.getSource();
		int newSize = IntegerArgumentType.getInteger(ctx, "size");

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("La isla de Spawn todavía no existe."));
			return 0;
		}

		try {
			IslandCoreMod.ISLAND_REGISTRY.resizeIsland(maybeIsland.get().getIslandId(), newSize);
		} catch (IllegalArgumentException e) {
			source.sendError(Text.literal(e.getMessage()));
			return 0;
		}

		source.sendFeedback(() -> Text.literal("Isla de Spawn ampliada a tamaño " + newSize + "."), false);

		return 1;
	}

	private static int executeAdminSpawnSetHome(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("La isla de Spawn todavía no existe. Usa /island admin spawn create primero."));
			return 0;
		}

		Island island = maybeIsland.get();
		boolean inIslandsDimension = player.getWorld().getRegistryKey().equals(ISLANDS_DIMENSION);
		boolean withinBuiltIsland = island.getBounds().contains(player.getBlockPos());

		if (!inIslandsDimension || !withinBuiltIsland) {
			source.sendError(Text.literal("El home de la isla de Spawn debe fijarse dentro de la parte ya construida de esa isla."));
			return 0;
		}

		BlockPos pos = player.getBlockPos();
		IslandCoreMod.ISLAND_REGISTRY.updateHomeLocation(island.getIslandId(), pos);

		source.sendFeedback(() -> Text.literal("Home de la isla de Spawn actualizado a ("
				+ pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")."), false);

		return 1;
	}

	private static int executeAdminSpawnBuildProtection(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource source = ctx.getSource();
		boolean value = BoolArgumentType.getBool(ctx, "value");

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("La isla de Spawn todavía no existe."));
			return 0;
		}

		IslandCoreMod.ISLAND_REGISTRY.updateIslandSetting(maybeIsland.get().getIslandId(), IslandSetting.BUILD_PROTECTION, value);

		source.sendFeedback(() -> Text.literal("Protección de construcción de la isla de Spawn: " + value + "."), false);

		return 1;
	}

	private static int executeAdminSpawnTrust(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("La isla de Spawn todavía no existe."));
			return 0;
		}

		MembershipService.trustOnIsland(maybeIsland.get(), target.getUuid());

		String targetName = target.getGameProfile().getName();
		source.sendFeedback(() -> Text.literal(targetName + " ahora puede construir siempre en la isla de Spawn."), false);

		return 1;
	}

	private static int executeAdminSpawnUntrust(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("La isla de Spawn todavía no existe."));
			return 0;
		}

		MembershipService.untrustOnIsland(maybeIsland.get(), target.getUuid());

		String targetName = target.getGameProfile().getName();
		source.sendFeedback(() -> Text.literal(targetName + " ya no tiene acceso especial en la isla de Spawn."), false);

		return 1;
	}

	private static int executeAdminDelete(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity admin = source.getPlayerOrThrow();
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(target.getUuid());
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal(target.getGameProfile().getName() + " no tiene ninguna isla."));
			return 0;
		}

		try {
			IslandCoreMod.DELETION_SERVICE.requestDeletion(maybeIsland.get().getIslandId(), admin.getUuid());
		} catch (IllegalArgumentException | IllegalStateException e) {
			source.sendError(Text.literal(e.getMessage()));
			return 0;
		}

		String targetName = target.getGameProfile().getName();
		source.sendFeedback(() -> Text.literal("¿Seguro que quieres borrar la isla de " + targetName
				+ "? Usa /island admin delete " + targetName + " confirm en los próximos 30 segundos."), false);

		return 1;
	}

	private static int executeAdminDeleteConfirm(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity admin = source.getPlayerOrThrow();
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(target.getUuid());
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal(target.getGameProfile().getName() + " no tiene ninguna isla."));
			return 0;
		}

		boolean confirmed = IslandCoreMod.DELETION_SERVICE.confirmDeletion(maybeIsland.get().getIslandId(), admin.getUuid());
		if (!confirmed) {
			source.sendError(Text.literal("No hay ninguna solicitud de borrado pendiente (o ha expirado)."));
			return 0;
		}

		String targetName = target.getGameProfile().getName();
		source.sendFeedback(() -> Text.literal("La isla de " + targetName + " se está borrando..."), false);

		return 1;
	}

}
