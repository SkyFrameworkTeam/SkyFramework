package com.skyframework.islandcore.command.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.island.IslandPermission;
import com.skyframework.islandcore.command.IslandMessages;
import com.skyframework.islandcore.island.generation.BasicPlatformGenerator;
import com.skyframework.islandcore.island.model.IslandMember;
import com.skyframework.islandcore.island.model.IslandRole;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.command.argument.EntityArgumentType;
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

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;

// TEMPORARY: delete once the real /island commands exist.
public class IslandDebugCommand {

	private static final RegistryKey<World> ISLANDS_DIMENSION =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

	private static final BasicPlatformGenerator PLATFORM_GENERATOR = new BasicPlatformGenerator();

	private IslandDebugCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LiteralCommandNode<ServerCommandSource> root = dispatcher.register(CommandManager.literal("islandcore")
					.then(CommandManager.literal("create")
							.requires(source -> source.hasPermissionLevel(2))
							.executes(IslandDebugCommand::executeCreate))
					.then(CommandManager.literal("list")
							.executes(IslandDebugCommand::executeList))
					.then(CommandManager.literal("info")
							.requires(source -> source.hasPermissionLevel(2))
							.executes(IslandDebugCommand::executeInfo))
					.then(CommandManager.literal("admin")
							.requires(source -> source.hasPermissionLevel(2))
							.then(CommandManager.literal("spawn")
									.then(CommandManager.literal("create")
											.then(CommandManager.argument("size", IntegerArgumentType.integer(1))
													.executes(IslandDebugCommand::executeAdminSpawnCreate)))
									.then(CommandManager.literal("resize")
											.then(CommandManager.argument("size", IntegerArgumentType.integer(1))
													.executes(IslandDebugCommand::executeAdminSpawnResize))))
							.then(CommandManager.literal("list")
									.executes(IslandDebugCommand::executeAdminListAll)
									.then(CommandManager.argument("player", EntityArgumentType.player())
											.executes(IslandDebugCommand::executeAdminListPlayer)))
							.then(CommandManager.literal("delete")
									.then(CommandManager.argument("player", EntityArgumentType.player())
											.executes(IslandDebugCommand::executeAdminDelete)
											.then(CommandManager.literal("confirm")
													.executes(IslandDebugCommand::executeAdminDeleteConfirm)))))
					// Minimal temporary trust/untrust: will be replaced by the real /island trust
					// and /island untrust (with proper validation and UX) in a future sprint.
					// Intentionally NOT gated behind hasPermissionLevel(2): any player manages their own island.
					.then(CommandManager.literal("trust")
							.then(CommandManager.argument("player", EntityArgumentType.player())
									.executes(IslandDebugCommand::executeTrust)))
					.then(CommandManager.literal("untrust")
							.then(CommandManager.argument("player", EntityArgumentType.player())
									.executes(IslandDebugCommand::executeUntrust)))
					// Temporary: lets us verify LuckPerms/fallback resolution as a normal player, not just an op.
					.then(CommandManager.literal("debug")
							.then(CommandManager.literal("perm")
									.then(CommandManager.argument("node", StringArgumentType.word())
											.executes(IslandDebugCommand::executeDebugPerm)))
							.then(CommandManager.literal("maxsize")
									.executes(IslandDebugCommand::executeDebugMaxSize))
							.then(CommandManager.literal("cooldown")
									.executes(IslandDebugCommand::executeDebugCooldown)))
			);

			dispatcher.register(CommandManager.literal("ic").redirect(root));
		});
	}

	private static int executeCreate(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		Island island;
		try {
			island = IslandCoreMod.ISLAND_REGISTRY.createIsland(player.getUuid(), ISLANDS_DIMENSION);
		} catch (IllegalStateException e) {
			source.sendError(Text.literal("Ya tienes una isla. Usa /ic info para ver sus datos."));
			return 0;
		}

		BlockPos center = island.getCenter();

		ServerWorld islandsWorld = source.getServer().getWorld(ISLANDS_DIMENSION);
		PLATFORM_GENERATOR.generate(islandsWorld, center, island.getIslandSize());

		source.sendFeedback(() -> Text.literal("Created island " + island.getIslandId()
				+ " at (" + center.getX() + ", " + center.getY() + ", " + center.getZ() + ")"), false);

		return 1;
	}

	private static int executeList(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUuid());
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("No tienes ninguna isla todavía. Usa /ic create para crear una."));
			return 0;
		}

		IslandMessages.sendIslandDetails(source, maybeIsland.get());
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

		IslandMessages.sendIslandDetails(source, maybeIsland.get());
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
				+ "? Usa /ic admin delete " + targetName + " confirm en los próximos 30 segundos."), false);

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

	private static int executeTrust(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity executor = source.getPlayerOrThrow();
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(executor.getUuid());
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("No tienes una isla."));
			return 0;
		}

		Island island = maybeIsland.get();
		IslandMember member = new IslandMember(target.getUuid(), IslandRole.TRUSTED, Instant.now(), EnumSet.noneOf(IslandPermission.class));
		IslandCoreMod.ISLAND_REGISTRY.addMember(island.getIslandId(), member);

		String targetName = target.getGameProfile().getName();
		source.sendFeedback(() -> Text.literal(targetName + " ahora es de confianza en tu isla."), false);

		return 1;
	}

	private static int executeUntrust(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity executor = source.getPlayerOrThrow();
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(executor.getUuid());
		if (maybeIsland.isEmpty()) {
			source.sendError(Text.literal("No tienes una isla."));
			return 0;
		}

		Island island = maybeIsland.get();
		IslandCoreMod.ISLAND_REGISTRY.removeMember(island.getIslandId(), target.getUuid());

		String targetName = target.getGameProfile().getName();
		source.sendFeedback(() -> Text.literal(targetName + " ya no tiene acceso especial a tu isla."), false);

		return 1;
	}

	private static int executeDebugPerm(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		String node = StringArgumentType.getString(ctx, "node");

		boolean result = IslandCoreMod.PERMISSION_PROVIDER.hasPermission(player.getUuid(), node);

		source.sendFeedback(() -> Text.literal(node + " = " + result), false);

		return 1;
	}

	private static int executeDebugMaxSize(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		int maxSize = IslandCoreMod.PERMISSION_PROVIDER.getHighestSizeAllowed(player.getUuid());

		source.sendFeedback(() -> Text.literal("maxSize = " + maxSize), false);

		return 1;
	}

	private static int executeDebugCooldown(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		int cooldownSeconds = IslandCoreMod.PERMISSION_PROVIDER.getHomeCooldownSeconds(player.getUuid());

		source.sendFeedback(() -> Text.literal("cooldownSeconds = " + cooldownSeconds), false);

		return 1;
	}
}
