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
import com.skyframework.islandcore.island.model.IslandRole;
import com.skyframework.islandcore.island.model.IslandSetting;
import com.skyframework.islandcore.protection.exception.ExceptionGroup;
import com.skyframework.islandcore.protection.exception.ExceptionGroupCategory;
import com.skyframework.islandcore.protection.exception.ExceptionResolver;
import com.skyframework.islandcore.protection.flag.Flag;
import com.skyframework.islandcore.protection.flag.FlagCategory;
import com.skyframework.islandcore.protection.flag.FlagPreset;
import com.skyframework.islandcore.protection.flag.FlagRegistry;
import com.skyframework.islandcore.protection.flag.FlagResolver;
import com.skyframework.islandcore.protection.flag.TriState;
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
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// The one and only /island command tree. The old /ic and /islandcore debug tree
// (command.debug.IslandDebugCommand) has been fully retired as of Sprint 12.
public class IslandCommand {

	private static final RegistryKey<World> ISLANDS_DIMENSION =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

	private static final List<String> SETTING_NAMES = List.of("firespread", "pvp", "mobdamage");
	private static final List<String> TRISTATE_NAMES = List.of("allow", "deny", "default");
	private static final List<String> FLAG_PRESET_NAMES = List.of("nadie", "miembros", "aliados", "todos");
	// "/island admin flags set-default" accepts either vocabulary depending on the flag's category
	// (ISLAND_GLOBAL -> allow/deny, ROLE_BASED -> preset) — see executeAdminFlagsSetDefault and
	// suggestFlagSetDefaultValues, which reads the already-typed "flag" argument to offer only the
	// vocabulary that flag actually accepts, instead of both unconditionally. This full union is
	// kept only as the fallback for an unrecognized/not-yet-typed flag id.
	private static final List<String> ALLOW_DENY_NAMES = List.of("allow", "deny");
	private static final List<String> FLAG_SET_DEFAULT_VALUE_NAMES =
			List.of("allow", "deny", "nadie", "miembros", "aliados", "todos");

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
						.then(CommandManager.literal("ally")
								.then(CommandManager.literal("add")
										.then(CommandManager.argument("player", EntityArgumentType.player())
												.executes(IslandCommand::executeAllyAdd)))
								.then(CommandManager.literal("remove")
										.then(CommandManager.argument("player", EntityArgumentType.player())
												.executes(IslandCommand::executeAllyRemove))))
						.then(CommandManager.literal("kick")
								.then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
										.executes(IslandCommand::executeKick)))
						.then(CommandManager.literal("biome")
								.then(CommandManager.argument("biome", IdentifierArgumentType.identifier())
										.suggests(IslandCommand::suggestBiomes)
										.executes(IslandCommand::executeBiome)))
						.then(CommandManager.literal("flags")
								.executes(IslandCommand::executeFlagsList)
								.then(CommandManager.literal("set")
										.then(CommandManager.argument("flag", StringArgumentType.word())
												.suggests(IslandCommand::suggestFlagIds)
												.then(CommandManager.argument("value", StringArgumentType.word())
														.suggests((ctx, builder) -> CommandSource.suggestMatching(TRISTATE_NAMES, builder))
														.executes(IslandCommand::executeFlagsSet))))
								.then(CommandManager.literal("preset")
										.then(CommandManager.argument("flag", StringArgumentType.word())
												.suggests(IslandCommand::suggestRoleBasedFlagIds)
												.then(CommandManager.argument("preset", StringArgumentType.word())
														.suggests((ctx, builder) -> CommandSource.suggestMatching(FLAG_PRESET_NAMES, builder))
														.executes(IslandCommand::executeFlagsPreset)))))
						.then(CommandManager.literal("exceptions")
								.then(CommandManager.literal("list")
										.executes(IslandCommand::executeExceptionsList))
								.then(CommandManager.literal("preset")
										.then(CommandManager.argument("group", StringArgumentType.word())
												.suggests(IslandCommand::suggestExceptionGroupIds)
												.then(CommandManager.argument("preset", StringArgumentType.word())
														.suggests((ctx, builder) -> CommandSource.suggestMatching(FLAG_PRESET_NAMES, builder))
														.executes(IslandCommand::executeExceptionsPreset)))))
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
														.executes(IslandCommand::executeAdminSpawnUntrust)))
										.then(CommandManager.literal("exceptions")
												.then(CommandManager.literal("preset")
														.then(CommandManager.argument("group", StringArgumentType.word())
																.suggests(IslandCommand::suggestExceptionGroupIds)
																.then(CommandManager.argument("preset", StringArgumentType.word())
																		.suggests((ctx, builder) -> CommandSource.suggestMatching(FLAG_PRESET_NAMES, builder))
																		.executes(IslandCommand::executeAdminSpawnExceptionsPreset))))))
								.then(CommandManager.literal("flags")
										.then(CommandManager.literal("set-default")
												.then(CommandManager.argument("flag", StringArgumentType.word())
														.suggests(IslandCommand::suggestFlagIds)
														.then(CommandManager.argument("value", StringArgumentType.word())
																.suggests(IslandCommand::suggestFlagSetDefaultValues)
																.executes(IslandCommand::executeAdminFlagsSetDefault))))
										.then(CommandManager.literal("require")
												.then(CommandManager.argument("flag", StringArgumentType.word())
														.suggests(IslandCommand::suggestFlagIds)
														.then(CommandManager.argument("permission", StringArgumentType.string())
																.executes(IslandCommand::executeAdminFlagsRequire)))))
								.then(CommandManager.literal("exceptions")
										.then(CommandManager.literal("set-default")
												.then(CommandManager.argument("group", StringArgumentType.word())
														.suggests(IslandCommand::suggestExceptionGroupIds)
														.then(CommandManager.argument("preset", StringArgumentType.word())
																.suggests((ctx, builder) -> CommandSource.suggestMatching(FLAG_PRESET_NAMES, builder))
																.executes(IslandCommand::executeAdminExceptionsSetDefault)))))
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

	// Delegates entirely to IslandActionService#updateLegacySetting — the SAME method
	// IslandSettingsUpdateC2S's network handler calls — so this command never decides on its own
	// whether a setting id maps to the new Flag system or the old IslandSetting one; that mapping
	// lives in exactly one place, and both paths stay convergent by construction.
	private static int executeSettings(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		String settingName = StringArgumentType.getString(ctx, "setting");
		boolean value = BoolArgumentType.getBool(ctx, "value");

		ActionOutcome<Void> outcome = IslandActionService.updateLegacySetting(player.getUuid(), settingName, value);
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendError(Text.literal("No tienes ninguna isla todavía."));
			} else if (ActionReason.UNKNOWN_SETTING.equals(outcome.reason())) {
				source.sendError(Text.literal("Ajuste desconocido. Ajustes disponibles: firespread, pvp, mobdamage."));
			} else {
				source.sendError(Text.literal("Solo el propietario de la isla puede cambiar sus ajustes."));
			}
			return 0;
		}

		source.sendFeedback(() -> Text.literal(settingName.toLowerCase() + " = " + value), false);

		return 1;
	}

	private static int executeFlagsList(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid());
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("No tienes ninguna isla todavía."));
			return 0;
		}

		Island island = maybeIsland.get();
		source.sendFeedback(() -> Text.literal("=== Flags de tu isla ===").formatted(Formatting.BOLD, Formatting.AQUA), false);

		IslandMessages.sectionTitle(source, "Permisos por Rol", Formatting.GOLD);
		IslandRole[] roles = IslandRole.values();
		for (Flag flag : FlagRegistry.all()) {
			if (flag.getCategory() != FlagCategory.ROLE_BASED) {
				continue;
			}

			String header = flagDisplayName(flag) + ":";
			source.sendFeedback(() -> Text.literal(header).formatted(Formatting.WHITE), false);

			// 3 roles per line (OWNER/MEMBER/CO_OWNER, then ALLY/VISITOR/DENIED) rather than one
			// long line: easier to scan, and each ALLOW/DENY value is colored green/red.
			for (int i = 0; i < roles.length; i += 3) {
				int lineEnd = Math.min(i + 3, roles.length);
				MutableText line = Text.literal("  ");
				for (int j = i; j < lineEnd; j++) {
					IslandRole role = roles[j];
					TriState value = FlagResolver.resolveForRole(island, role, flag);
					line.append(roleValueText(role, value));
					if (j < lineEnd - 1) {
						line.append(Text.literal("   "));
					}
				}
				source.sendFeedback(() -> line, false);
			}
		}

		IslandMessages.sectionTitle(source, "Ajustes Generales", Formatting.LIGHT_PURPLE);
		for (Flag flag : FlagRegistry.all()) {
			if (flag.getCategory() != FlagCategory.ISLAND_GLOBAL) {
				continue;
			}
			String valueText = FlagResolver.resolveGlobal(island, flag) ? "allow" : "deny";
			String label = flagDisplayName(flag) + ": ";
			source.sendFeedback(() -> IslandMessages.labeled(label, valueText), false);
		}

		return 1;
	}

	private static Text roleValueText(IslandRole role, TriState value) {
		Formatting valueColor = value == TriState.ALLOW ? Formatting.GREEN : Formatting.RED;
		return Text.literal(role.name() + ": ").formatted(Formatting.GRAY)
				.append(Text.literal(value.name()).formatted(valueColor));
	}

	// Presentation only (Sprint "mejora visual de /island flags") — the flag ids themselves
	// (used by /island flags set and stored on disk) are unchanged.
	private static String flagDisplayName(Flag flag) {
		return switch (flag.getId()) {
			case "construccion" -> "Construcción";
			case "interact" -> "Interactuar";
			case "entities" -> "Entidades";
			case "fire_spread" -> "Propagación de fuego";
			case "pvp_damage" -> "Daño PvP";
			case "mob_damage" -> "Daño de mobs";
			case "explosion_damage" -> "Daño de explosiones";
			case "crop_trample" -> "Pisoteo de cultivos";
			case "natural_mob_spawning" -> "Aparición natural de mobs";
			case "raids" -> "Incursiones (raids)";
			default -> flag.getId();
		};
	}

	private static int executeFlagsSet(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		String flagId = StringArgumentType.getString(ctx, "flag");
		String valueArg = StringArgumentType.getString(ctx, "value");

		Optional<Flag> maybeFlag = FlagRegistry.get(flagId);
		if (maybeFlag.isEmpty()) {
			source.sendError(Text.literal("Flag desconocido: " + flagId));
			return 0;
		}

		TriState value;
		try {
			value = TriState.valueOf(valueArg.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			source.sendError(Text.literal("Valor inválido: " + valueArg + ". Usa allow, deny o default."));
			return 0;
		}

		ActionOutcome<Void> outcome = IslandActionService.updateFlag(player.getUuid(), maybeFlag.get(), value);
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendError(Text.literal("No tienes ninguna isla todavía."));
			} else if (ActionReason.MISSING_FLAG_PERMISSION.equals(outcome.reason())) {
				source.sendError(Text.literal(missingFlagPermissionMessage(flagId)));
			} else {
				source.sendError(Text.literal("Solo el propietario de la isla puede cambiar sus flags."));
			}
			return 0;
		}

		source.sendFeedback(() -> Text.literal(flagId + " = " + valueArg.toLowerCase(Locale.ROOT)), false);

		return 1;
	}

	private static int executeFlagsPreset(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		String flagId = StringArgumentType.getString(ctx, "flag");
		String preset = StringArgumentType.getString(ctx, "preset");

		ActionOutcome<Void> outcome = IslandActionService.applyFlagPreset(player.getUuid(), flagId, preset);
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendError(Text.literal("No tienes ninguna isla todavía."));
			} else if (ActionReason.INVALID_FLAG_PRESET.equals(outcome.reason())) {
				source.sendError(Text.literal(
						"Flag o preset inválido. Usa un flag por rol (no fire_spread/pvp_damage/mob_damage) y uno de: "
								+ String.join(", ", FLAG_PRESET_NAMES) + "."));
			} else if (ActionReason.MISSING_FLAG_PERMISSION.equals(outcome.reason())) {
				source.sendError(Text.literal(missingFlagPermissionMessage(flagId)));
			} else {
				source.sendError(Text.literal("Solo el propietario de la isla puede cambiar sus flags."));
			}
			return 0;
		}

		source.sendFeedback(() -> Text.literal(flagId + " = preset " + preset), false);

		return 1;
	}

	// Shared by executeFlagsSet/executeFlagsPreset: ActionReason.MISSING_FLAG_PERMISSION alone
	// doesn't carry which node was missing (the wire-facing ActionReason keys are static strings,
	// no parameters), so the text-command path re-looks it up here to give a concrete message —
	// the network path's client just sees the generic reason key, translated client-side.
	private static String missingFlagPermissionMessage(String flagId) {
		String node = IslandCoreMod.FLAG_PERMISSION_REQUIREMENTS.getRequiredPermission(flagId).orElse("?");
		return "No tienes el permiso necesario para cambiar " + flagId + " (\"" + node + "\").";
	}

	// Dispatches by the flag's own category: ISLAND_GLOBAL flags (fire_spread/pvp_damage/mob_damage)
	// keep the plain allow/deny value they always had (a preset is meaningless for a flag with no
	// per-role distinction); ROLE_BASED flags now require one of the 4 preset names instead — a flat
	// single value can't express "different per role" the way a preset can.
	private static int executeAdminFlagsSetDefault(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource source = ctx.getSource();
		String flagId = StringArgumentType.getString(ctx, "flag");
		String valueArg = StringArgumentType.getString(ctx, "value");

		Optional<Flag> maybeFlag = FlagRegistry.get(flagId);
		if (maybeFlag.isEmpty()) {
			source.sendError(Text.literal("Flag desconocido: " + flagId));
			return 0;
		}
		Flag flag = maybeFlag.get();

		if (flag.getCategory() == FlagCategory.ISLAND_GLOBAL) {
			TriState value;
			try {
				value = TriState.valueOf(valueArg.toUpperCase(Locale.ROOT));
				if (value == TriState.DEFAULT) {
					throw new IllegalArgumentException("default no es válido aquí");
				}
			} catch (IllegalArgumentException e) {
				source.sendError(Text.literal("Valor inválido: " + valueArg + ". Usa allow o deny (este flag no admite preset)."));
				return 0;
			}

			IslandCoreMod.SERVER_FLAG_DEFAULTS.setGlobalDefault(flagId, value);
			source.sendFeedback(() -> Text.literal(
					"Valor por defecto del servidor para " + flagId + " = " + valueArg.toLowerCase(Locale.ROOT)), false);
			return 1;
		}

		Optional<FlagPreset> preset = FlagPreset.fromId(valueArg.toLowerCase(Locale.ROOT));
		if (preset.isEmpty()) {
			source.sendError(Text.literal("Valor inválido: " + valueArg + ". Usa uno de: "
					+ String.join(", ", FLAG_PRESET_NAMES) + " (este flag es por rol, no admite allow/deny)."));
			return 0;
		}

		IslandCoreMod.SERVER_FLAG_DEFAULTS.setRoleBasedDefault(flagId, preset.get());
		source.sendFeedback(() -> Text.literal(
				"Valor por defecto del servidor para " + flagId + " = preset " + preset.get().getId()), false);
		return 1;
	}

	private static int executeAdminFlagsRequire(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource source = ctx.getSource();
		String flagId = StringArgumentType.getString(ctx, "flag");
		String permissionArg = StringArgumentType.getString(ctx, "permission");

		if (FlagRegistry.get(flagId).isEmpty()) {
			source.sendError(Text.literal("Flag desconocido: " + flagId));
			return 0;
		}

		String node = "ninguno".equalsIgnoreCase(permissionArg) ? null : permissionArg;
		IslandCoreMod.FLAG_PERMISSION_REQUIREMENTS.setRequiredPermission(flagId, node);

		source.sendFeedback(() -> Text.literal(node == null
				? "El flag " + flagId + " ya no requiere ningún permiso especial para cambiarlo."
				: "El flag " + flagId + " ahora requiere el permiso \"" + node + "\" para cambiarlo."), false);

		return 1;
	}

	private static int executeExceptionsList(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid());
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("No tienes ninguna isla todavía."));
			return 0;
		}

		Island island = maybeIsland.get();
		List<ExceptionGroup> groups = IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getAllGroups();
		if (groups.isEmpty()) {
			source.sendFeedback(() -> Text.literal("No hay ningún grupo de excepción configurado."), false);
			return 0;
		}

		source.sendFeedback(() -> Text.literal("=== Grupos de Excepción de tu Isla ===").formatted(Formatting.BOLD, Formatting.AQUA), false);

		sendExceptionGroupSection(source, island, groups, ExceptionGroupCategory.BLOCK, "Bloques");
		sendExceptionGroupSection(source, island, groups, ExceptionGroupCategory.ENTITY, "Entidades");

		return groups.size();
	}

	// Same titled-section look as executeFlagsList (see IslandMessages#sectionTitle), split by
	// ExceptionGroupCategory (BLOCK/ENTITY) instead of by role since exception groups have no role
	// axis of their own — but now resolve per role too (see ExceptionResolver), so each group shows
	// its current preset plus the same 6-role breakdown executeFlagsList already prints for a
	// ROLE_BASED flag. A section with no groups is skipped entirely rather than printed empty.
	private static void sendExceptionGroupSection(
			ServerCommandSource source, Island island, List<ExceptionGroup> groups, ExceptionGroupCategory category, String title) {
		List<ExceptionGroup> inCategory = groups.stream().filter(group -> group.getCategory() == category).toList();
		if (inCategory.isEmpty()) {
			return;
		}

		IslandMessages.sectionTitle(source, title, Formatting.GOLD);
		IslandRole[] roles = IslandRole.values();
		for (ExceptionGroup group : inCategory) {
			String configurableSuffix = group.isOwnerConfigurable() ? "" : " (solo gestionable por un admin)";
			String currentPreset = ExceptionResolver.currentPreset(island, group);

			MutableText header = Text.literal(group.getId()).formatted(Formatting.WHITE)
					.append(Text.literal(": preset " + currentPreset).formatted(Formatting.GRAY))
					.append(Text.literal(configurableSuffix).formatted(Formatting.GRAY));
			source.sendFeedback(() -> header, false);

			for (int i = 0; i < roles.length; i += 3) {
				int lineEnd = Math.min(i + 3, roles.length);
				MutableText line = Text.literal("  ");
				for (int j = i; j < lineEnd; j++) {
					IslandRole role = roles[j];
					boolean enabled = ExceptionResolver.isEnabledForRole(island, role, group);
					line.append(exceptionRoleValueText(role, enabled));
					if (j < lineEnd - 1) {
						line.append(Text.literal("   "));
					}
				}
				source.sendFeedback(() -> line, false);
			}
		}
	}

	private static Text exceptionRoleValueText(IslandRole role, boolean enabled) {
		Formatting valueColor = enabled ? Formatting.GREEN : Formatting.RED;
		return Text.literal(role.name() + ": ").formatted(Formatting.GRAY)
				.append(Text.literal(enabled ? "ON" : "OFF").formatted(valueColor));
	}

	private static int executeExceptionsPreset(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		String groupId = StringArgumentType.getString(ctx, "group");
		String preset = StringArgumentType.getString(ctx, "preset");

		Optional<ExceptionGroup> maybeGroup = IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getGroup(groupId);
		if (maybeGroup.isEmpty()) {
			source.sendError(Text.literal("Grupo de excepción desconocido: " + groupId));
			return 0;
		}

		if (!maybeGroup.get().isOwnerConfigurable()) {
			source.sendError(Text.literal("El grupo " + groupId + " solo puede gestionarlo un administrador."));
			return 0;
		}

		ActionOutcome<Void> outcome = IslandActionService.applyExceptionGroupPreset(player.getUuid(), groupId, preset);
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendError(Text.literal("No tienes ninguna isla todavía."));
			} else if (ActionReason.INVALID_EXCEPTION_PRESET.equals(outcome.reason())) {
				source.sendError(Text.literal("Preset inválido. Usa uno de: " + String.join(", ", FLAG_PRESET_NAMES) + "."));
			} else {
				source.sendError(Text.literal("Solo el propietario de la isla puede cambiar sus grupos de excepción."));
			}
			return 0;
		}

		source.sendFeedback(() -> Text.literal(groupId + " = preset " + preset), false);

		return 1;
	}

	private static int executeAdminExceptionsSetDefault(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource source = ctx.getSource();
		String groupId = StringArgumentType.getString(ctx, "group");
		String presetArg = StringArgumentType.getString(ctx, "preset");

		if (IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getGroup(groupId).isEmpty()) {
			source.sendError(Text.literal("Grupo de excepción desconocido: " + groupId));
			return 0;
		}

		Optional<FlagPreset> preset = FlagPreset.fromId(presetArg.toLowerCase(Locale.ROOT));
		if (preset.isEmpty()) {
			source.sendError(Text.literal("Preset inválido: " + presetArg + ". Usa uno de: " + String.join(", ", FLAG_PRESET_NAMES) + "."));
			return 0;
		}

		IslandCoreMod.SERVER_EXCEPTION_DEFAULTS.setDefault(groupId, preset.get());

		source.sendFeedback(() -> Text.literal(
				"Valor por defecto del servidor para el grupo " + groupId + " = preset " + preset.get().getId()), false);

		return 1;
	}

	private static int executeAdminSpawnExceptionsPreset(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource source = ctx.getSource();
		String groupId = StringArgumentType.getString(ctx, "group");
		String presetArg = StringArgumentType.getString(ctx, "preset");

		if (IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getGroup(groupId).isEmpty()) {
			source.sendError(Text.literal("Grupo de excepción desconocido: " + groupId));
			return 0;
		}

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("La isla de Spawn todavía no existe."));
			return 0;
		}

		try {
			IslandCoreMod.ISLAND_REGISTRY.applyExceptionGroupPreset(maybeIsland.get().getIslandId(), groupId, presetArg.toLowerCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			source.sendError(Text.literal("Preset inválido: " + presetArg + ". Usa uno de: " + String.join(", ", FLAG_PRESET_NAMES) + "."));
			return 0;
		}

		source.sendFeedback(() -> Text.literal(groupId + " = preset " + presetArg.toLowerCase(Locale.ROOT) + " en la isla de Spawn."), false);

		return 1;
	}

	private static CompletableFuture<Suggestions> suggestRoleBasedFlagIds(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
		List<String> ids = FlagRegistry.all().stream()
				.filter(flag -> flag.getCategory() == FlagCategory.ROLE_BASED)
				.map(Flag::getId)
				.toList();
		return CommandSource.suggestMatching(ids, builder);
	}

	private static CompletableFuture<Suggestions> suggestFlagIds(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
		List<String> ids = FlagRegistry.all().stream().map(Flag::getId).toList();
		return CommandSource.suggestMatching(ids, builder);
	}

	private static CompletableFuture<Suggestions> suggestExceptionGroupIds(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
		List<String> ids = IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getAllGroups().stream().map(ExceptionGroup::getId).toList();
		return CommandSource.suggestMatching(ids, builder);
	}

	// Reads the "flag" argument already typed earlier in this same command (Brigadier resolves
	// arguments left-to-right, so it's guaranteed parsed by the time suggestions run for "value")
	// to offer only the vocabulary that flag's category actually accepts — allow/deny for
	// ISLAND_GLOBAL, the 4 presets for ROLE_BASED — instead of the full union of both regardless of
	// which flag was typed. Falls back to the full union for an unrecognized or not-yet-typed flag
	// id, so suggestions still show something reasonable while the player is still typing "flag".
	private static CompletableFuture<Suggestions> suggestFlagSetDefaultValues(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
		String flagId = StringArgumentType.getString(ctx, "flag");
		List<String> names = FlagRegistry.get(flagId)
				.map(flag -> flag.getCategory() == FlagCategory.ISLAND_GLOBAL ? ALLOW_DENY_NAMES : FLAG_PRESET_NAMES)
				.orElse(FLAG_SET_DEFAULT_VALUE_NAMES);
		return CommandSource.suggestMatching(names, builder);
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
		source.sendFeedback(() -> Text.literal(targetName + " ahora es copropietario de tu isla."), false);

		return 1;
	}

	private static int executeUntrust(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity executor = source.getPlayerOrThrow();
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
		String targetName = target.getGameProfile().getName();

		ActionOutcome<Void> outcome = MembershipService.untrust(executor, target.getUuid());
		if (!outcome.success()) {
			if (ActionReason.NOT_CO_OWNER.equals(outcome.reason())) {
				source.sendError(Text.literal(targetName + " no es copropietario de tu isla."));
			} else {
				source.sendError(Text.literal("No tienes una isla."));
			}
			return 0;
		}

		source.sendFeedback(() -> Text.literal(targetName + " ya no es copropietario de tu isla (sigue siendo miembro)."), false);

		return 1;
	}

	private static int executeAllyAdd(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity executor = source.getPlayerOrThrow();
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

		ActionOutcome<Void> outcome = MembershipService.allyAdd(executor, target.getUuid());
		if (!outcome.success()) {
			source.sendError(Text.literal("No tienes una isla."));
			return 0;
		}

		String targetName = target.getGameProfile().getName();
		source.sendFeedback(() -> Text.literal(targetName + " ahora es aliado de tu isla."), false);

		return 1;
	}

	private static int executeAllyRemove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity executor = source.getPlayerOrThrow();
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

		ActionOutcome<Void> outcome = MembershipService.allyRemove(executor, target.getUuid());
		if (!outcome.success()) {
			source.sendError(Text.literal("No tienes una isla."));
			return 0;
		}

		String targetName = target.getGameProfile().getName();
		source.sendFeedback(() -> Text.literal(targetName + " ya no es aliado de tu isla."), false);

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
