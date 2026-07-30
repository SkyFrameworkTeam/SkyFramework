package com.skyframework.islandcore.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.dimension.model.DimensionDefinition;
import com.skyframework.islandcore.dimension.model.DimensionGeneratorStyle;
import com.skyframework.islandcore.dimension.vanilla.PendingVanillaReset;
import com.skyframework.islandcore.dimension.vanilla.VanillaResetService;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

// Admin-only ("/dimension", requires permission level 2) commands for the Dimension Manager.
public class DimensionCommand {

	private DimensionCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("dimension")
						.requires(source -> source.hasPermissionLevel(2))
						.then(CommandManager.literal("create")
								.then(CommandManager.argument("id", StringArgumentType.word())
										.then(CommandManager.argument("displayName", StringArgumentType.string())
												.then(CommandManager.argument("style", StringArgumentType.word())
														.suggests(DimensionCommand::suggestStyles)
														.executes(ctx -> executeCreate(ctx, null))
														.then(CommandManager.argument("seed", LongArgumentType.longArg())
																.executes(ctx -> executeCreate(ctx, LongArgumentType.getLong(ctx, "seed"))))))))
						.then(CommandManager.literal("list")
								.executes(DimensionCommand::executeList))
						.then(CommandManager.literal("info")
								.then(CommandManager.argument("id", StringArgumentType.word())
										.suggests(DimensionCommand::suggestExistingIds)
										.executes(DimensionCommand::executeInfo)))
						.then(CommandManager.literal("delete")
								.then(CommandManager.argument("id", StringArgumentType.word())
										.suggests(DimensionCommand::suggestExistingIds)
										.executes(DimensionCommand::executeDelete)
										.then(CommandManager.literal("confirm")
												.executes(DimensionCommand::executeDeleteConfirm))))
						.then(CommandManager.literal("regenerate")
								.then(CommandManager.argument("id", StringArgumentType.word())
										.suggests(DimensionCommand::suggestExistingIds)
										.executes(ctx -> executeRegenerate(ctx, null))
										.then(CommandManager.argument("seed", LongArgumentType.longArg())
												.executes(ctx -> executeRegenerate(ctx, LongArgumentType.getLong(ctx, "seed"))))
										.then(CommandManager.literal("confirm")
												.executes(DimensionCommand::executeRegenerateConfirm))))
						.then(CommandManager.literal("vanilla")
								.then(CommandManager.literal("regenerate")
										.then(CommandManager.argument("dimensionKey", StringArgumentType.word())
												.suggests(DimensionCommand::suggestVanillaDimensionKeys)
												.executes(ctx -> executeVanillaRegenerate(ctx, null))
												.then(CommandManager.argument("seed", LongArgumentType.longArg())
														.executes(ctx -> executeVanillaRegenerate(ctx, LongArgumentType.getLong(ctx, "seed"))))
												.then(CommandManager.literal("confirm")
														.executes(DimensionCommand::executeVanillaRegenerateConfirm))))
								.then(CommandManager.literal("cancel")
										.then(CommandManager.argument("dimensionKey", StringArgumentType.word())
												.suggests(DimensionCommand::suggestVanillaDimensionKeys)
												.executes(DimensionCommand::executeVanillaCancel)))
								.then(CommandManager.literal("list")
										.executes(DimensionCommand::executeVanillaList)))
				)
		);
	}

	private static int executeCreate(CommandContext<ServerCommandSource> ctx, Long explicitSeed) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		String idPath = StringArgumentType.getString(ctx, "id");
		String displayName = StringArgumentType.getString(ctx, "displayName");
		String styleArg = StringArgumentType.getString(ctx, "style");

		DimensionGeneratorStyle style;
		try {
			style = DimensionGeneratorStyle.valueOf(styleArg.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			source.sendError(Text.literal("Estilo desconocido: " + styleArg
					+ ". Usa uno de: " + Arrays.toString(DimensionGeneratorStyle.values())));
			return 0;
		}

		Identifier id;
		try {
			id = Identifier.of("islandcore", idPath);
		} catch (RuntimeException e) {
			source.sendError(Text.literal("Id inválido: \"" + idPath
					+ "\" (solo minúsculas, números, '_', '-' y '/')."));
			return 0;
		}

		long seed = explicitSeed != null ? explicitSeed : new Random().nextLong();

		DimensionDefinition dimension;
		try {
			dimension = IslandCoreMod.DIMENSION_REGISTRY.createDimension(id, displayName, style, seed);
		} catch (IllegalStateException e) {
			source.sendError(Text.literal(e.getMessage()));
			return 0;
		}

		source.sendFeedback(() -> Text.literal("Dimensión creada: " + dimension.getId()
				+ " (\"" + dimension.getDisplayName() + "\", estilo " + dimension.getGeneratorStyle()
				+ ", semilla " + dimension.getSeed() + ")."), false);

		return 1;
	}

	private static int executeList(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource source = ctx.getSource();
		Collection<DimensionDefinition> dimensions = IslandCoreMod.DIMENSION_REGISTRY.getAllDimensions();

		if (dimensions.isEmpty()) {
			source.sendFeedback(() -> Text.literal("No hay dimensiones gestionadas todavía."), false);
			return 0;
		}

		source.sendFeedback(() -> Text.literal("Dimensiones (" + dimensions.size() + "):"), false);
		for (DimensionDefinition dimension : dimensions) {
			source.sendFeedback(() -> Text.literal("- " + dimension.getId() + " \"" + dimension.getDisplayName() + "\""
					+ " estilo=" + dimension.getGeneratorStyle()
					+ " semilla=" + dimension.getSeed()
					+ " estado=" + dimension.getState()), false);
		}

		return dimensions.size();
	}

	private static int executeInfo(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		String idPath = StringArgumentType.getString(ctx, "id");
		Identifier id = Identifier.of("islandcore", idPath);

		Optional<DimensionDefinition> maybeDimension = IslandCoreMod.DIMENSION_REGISTRY.getDimension(id);
		if (maybeDimension.isEmpty()) {
			source.sendError(Text.literal("No existe ninguna dimensión gestionada con id " + id + "."));
			return 0;
		}

		DimensionDefinition dimension = maybeDimension.get();
		source.sendFeedback(() -> Text.literal("id=" + dimension.getId()), false);
		source.sendFeedback(() -> Text.literal("displayName=" + dimension.getDisplayName()), false);
		source.sendFeedback(() -> Text.literal("generatorStyle=" + dimension.getGeneratorStyle()), false);
		source.sendFeedback(() -> Text.literal("seed=" + dimension.getSeed()), false);
		source.sendFeedback(() -> Text.literal("state=" + dimension.getState()), false);
		source.sendFeedback(() -> Text.literal("createdAt=" + dimension.getCreatedAt()), false);
		source.sendFeedback(() -> Text.literal("updatedAt=" + dimension.getUpdatedAt()), false);

		return 1;
	}

	private static int executeDelete(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		Identifier id = Identifier.of("islandcore", StringArgumentType.getString(ctx, "id"));

		try {
			IslandCoreMod.DIMENSION_REGISTRY.requestDeletion(id, player.getUuid());
		} catch (IllegalArgumentException | IllegalStateException e) {
			source.sendError(Text.literal(e.getMessage()));
			return 0;
		}

		source.sendFeedback(() -> Text.literal("¿Seguro que quieres borrar la dimensión " + id
				+ "? Esta acción no se puede deshacer. Usa /dimension delete " + id.getPath()
				+ " confirm en los próximos 30 segundos para confirmar."), false);

		return 1;
	}

	private static int executeDeleteConfirm(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		Identifier id = Identifier.of("islandcore", StringArgumentType.getString(ctx, "id"));

		boolean confirmed = IslandCoreMod.DIMENSION_REGISTRY.confirmDeletion(id, player.getUuid());
		if (!confirmed) {
			source.sendError(Text.literal("No hay ninguna solicitud de borrado pendiente para " + id + " (o ha expirado)."));
			return 0;
		}

		source.sendFeedback(() -> Text.literal("La dimensión " + id + " se está borrando..."), false);

		return 1;
	}

	private static int executeRegenerate(CommandContext<ServerCommandSource> ctx, Long explicitSeed) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		Identifier id = Identifier.of("islandcore", StringArgumentType.getString(ctx, "id"));

		long newSeed = explicitSeed != null ? explicitSeed : new Random().nextLong();

		try {
			IslandCoreMod.DIMENSION_REGISTRY.requestRegeneration(id, player.getUuid(), newSeed);
		} catch (IllegalArgumentException | IllegalStateException e) {
			source.sendError(Text.literal(e.getMessage()));
			return 0;
		}

		source.sendFeedback(() -> Text.literal("¿Seguro que quieres regenerar la dimensión " + id
				+ " con la semilla " + newSeed + "? Todo lo construido en ella se perderá. Usa /dimension regenerate "
				+ id.getPath() + " confirm en los próximos 30 segundos para confirmar."), false);

		return 1;
	}

	private static int executeRegenerateConfirm(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		Identifier id = Identifier.of("islandcore", StringArgumentType.getString(ctx, "id"));

		boolean confirmed = IslandCoreMod.DIMENSION_REGISTRY.confirmRegeneration(id, player.getUuid());
		if (!confirmed) {
			source.sendError(Text.literal("No hay ninguna solicitud de regeneración pendiente para " + id + " (o ha expirado)."));
			return 0;
		}

		source.sendFeedback(() -> Text.literal("La dimensión " + id + " se está regenerando..."), false);

		return 1;
	}

	private static int executeVanillaRegenerate(CommandContext<ServerCommandSource> ctx, Long explicitSeed) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		String dimensionKey = StringArgumentType.getString(ctx, "dimensionKey");

		try {
			IslandCoreMod.VANILLA_RESET_SERVICE.requestReset(dimensionKey, player.getUuid(), explicitSeed);
		} catch (IllegalArgumentException e) {
			source.sendError(Text.literal(e.getMessage()));
			return 0;
		}

		source.sendFeedback(() -> Text.literal("¿Seguro que quieres resetear la dimensión vanilla " + dimensionKey
				+ "? Esta acción no se puede deshacer. Usa /dimension vanilla regenerate " + dimensionKey
				+ " confirm en los próximos 30 segundos para confirmar."), false);

		return 1;
	}

	private static int executeVanillaRegenerateConfirm(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		String dimensionKey = StringArgumentType.getString(ctx, "dimensionKey");

		boolean confirmed;
		try {
			confirmed = IslandCoreMod.VANILLA_RESET_SERVICE.confirmReset(dimensionKey, player.getUuid());
		} catch (IllegalArgumentException e) {
			source.sendError(Text.literal(e.getMessage()));
			return 0;
		}

		if (!confirmed) {
			source.sendError(Text.literal("No hay ninguna solicitud de reseteo pendiente para " + dimensionKey + " (o ha expirado)."));
			return 0;
		}

		source.sendFeedback(() -> Text.literal("Reseteo de " + dimensionKey + " añadido a la cola de reseteos pendientes"
				+ " (junto a cualquier otro ya solicitado, si lo hay). Se aplicará en el PRÓXIMO reinicio del servidor:"
				+ " el mod no puede reiniciarlo por sí mismo, así que debes pararlo y volver a arrancarlo tú mismo"
				+ " cuando quieras que se apliquen todos los reseteos en cola."), false);

		return 1;
	}

	private static int executeVanillaCancel(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		String dimensionKey = StringArgumentType.getString(ctx, "dimensionKey");

		boolean cancelled;
		try {
			cancelled = IslandCoreMod.VANILLA_RESET_SERVICE.cancelPendingReset(dimensionKey);
		} catch (IllegalArgumentException e) {
			source.sendError(Text.literal(e.getMessage()));
			return 0;
		}

		if (!cancelled) {
			source.sendError(Text.literal("No hay ninguna solicitud de reseteo pendiente para " + dimensionKey + " en la cola."));
			return 0;
		}

		source.sendFeedback(() -> Text.literal("Reseteo pendiente de " + dimensionKey
				+ " cancelado. No se aplicará en el próximo reinicio."), false);

		return 1;
	}

	private static int executeVanillaList(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource source = ctx.getSource();
		List<PendingVanillaReset> queue = IslandCoreMod.VANILLA_RESET_SERVICE.listPendingResets();

		if (queue.isEmpty()) {
			source.sendFeedback(() -> Text.literal("No hay ningún reseteo vanilla pendiente en la cola."), false);
			return 0;
		}

		source.sendFeedback(() -> Text.literal("Reseteos vanilla pendientes (" + queue.size() + "):"), false);
		for (PendingVanillaReset pending : queue) {
			String seedText = pending.getSeed() != null ? String.valueOf(pending.getSeed()) : "mantiene la actual";
			source.sendFeedback(() -> Text.literal("- ")
					.append(vanillaDimensionDisplayName(pending.getDimensionKey()))
					.append(Text.literal(" | semilla=" + seedText + " | solicitado por " + pending.getRequestedBy())), false);
		}

		return queue.size();
	}

	// Bold + a color of its own per dimension, matching IslandMessages' labeled-value pattern
	// (colored/bold piece via .formatted(...), plain rest of the line appended alongside it).
	private static Text vanillaDimensionDisplayName(String dimensionKey) {
		return switch (dimensionKey) {
			case "overworld" -> Text.literal("Overworld").formatted(Formatting.BOLD, Formatting.GREEN);
			case "nether" -> Text.literal("Nether").formatted(Formatting.BOLD, Formatting.RED);
			case "end" -> Text.literal("End").formatted(Formatting.BOLD, Formatting.LIGHT_PURPLE);
			default -> Text.literal(dimensionKey);
		};
	}

	private static CompletableFuture<Suggestions> suggestVanillaDimensionKeys(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
		return CommandSource.suggestMatching(VanillaResetService.VALID_DIMENSION_KEYS, builder);
	}

	private static CompletableFuture<Suggestions> suggestStyles(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
		List<String> names = Arrays.stream(DimensionGeneratorStyle.values()).map(Enum::name).toList();
		return CommandSource.suggestMatching(names, builder);
	}

	private static CompletableFuture<Suggestions> suggestExistingIds(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
		List<String> paths = IslandCoreMod.DIMENSION_REGISTRY.getAllDimensions().stream()
				.map(dimension -> dimension.getId().getPath())
				.toList();
		return CommandSource.suggestMatching(paths, builder);
	}
}
