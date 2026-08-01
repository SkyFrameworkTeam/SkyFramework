package com.skyframework.islandcore;

import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.permission.PermissionProvider;
import com.skyframework.islandcore.api.registry.IslandRegistryApi;
import com.skyframework.islandcore.command.DimensionCommand;
import com.skyframework.islandcore.command.IslandCommand;
import com.skyframework.islandcore.dimension.registry.DimensionRegistry;
import com.skyframework.islandcore.dimension.registry.DimensionRegistryImpl;
import com.skyframework.islandcore.dimension.runtime.FantasyDimensionRuntimeProvider;
import com.skyframework.islandcore.dimension.vanilla.VanillaResetConfig;
import com.skyframework.islandcore.dimension.vanilla.VanillaResetExecutor;
import com.skyframework.islandcore.dimension.vanilla.VanillaResetService;
import com.skyframework.islandcore.farming.FarmingCommand;
import com.skyframework.islandcore.farming.FarmingConfig;
import com.skyframework.islandcore.island.biome.BiomeTierRegistry;
import com.skyframework.islandcore.island.biome.BiomeTierRegistryImpl;
import com.skyframework.islandcore.island.biome.IslandBiomeApplier;
import com.skyframework.islandcore.island.entity.IslandEntityTracker;
import com.skyframework.islandcore.island.entity.IslandEntityTrackerImpl;
import com.skyframework.islandcore.island.lifecycle.IslandDeletionService;
import com.skyframework.islandcore.island.lifecycle.IslandDeletionServiceImpl;
import com.skyframework.islandcore.island.lifecycle.InviteManager;
import com.skyframework.islandcore.island.lifecycle.InviteManagerImpl;
import com.skyframework.islandcore.island.registry.IslandRegistryImpl;
import com.skyframework.islandcore.net.ClientSyncNotifier;
import com.skyframework.islandcore.net.ServerPacketHandlers;
import com.skyframework.islandcore.permission.FallbackPermissionProvider;
import com.skyframework.islandcore.permission.LuckPermsProvider;
import com.skyframework.islandcore.player.FirstJoinTracker;
import com.skyframework.islandcore.player.StarterKitConfig;
import com.skyframework.islandcore.portal.PortalLinkConfig;
import com.skyframework.islandcore.protection.AccessController;
import com.skyframework.islandcore.protection.AccessControllerImpl;
import com.skyframework.islandcore.protection.DamageProtectionListener;
import com.skyframework.islandcore.protection.ProtectionListeners;
import com.skyframework.islandcore.rtp.RtpCommand;
import com.skyframework.islandcore.rtp.RtpConfig;
import com.skyframework.islandcore.spawn.SpawnCommand;
import com.skyframework.islandcore.spawn.SpawnConfig;
import com.skyframework.islandcore.teleport.TeleportManager;
import com.skyframework.islandcore.teleport.TeleportManagerImpl;
import com.skyframework.islandcore.teleport.VanillaTeleportBackend;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class IslandCoreMod implements ModInitializer {
	public static final String MOD_ID = "islandcore";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Static access is temporary for this phase of development; will be revisited
	// once a proper dependency-injection/service-locator pattern is needed.
	public static IslandRegistryApi ISLAND_REGISTRY;
	public static AccessController ACCESS_CONTROLLER;
	public static PermissionProvider PERMISSION_PROVIDER;
	public static TeleportManager TELEPORT_MANAGER;
	public static IslandDeletionService DELETION_SERVICE;
	public static InviteManager INVITE_MANAGER;
	public static BiomeTierRegistry BIOME_TIER_REGISTRY;
	public static IslandBiomeApplier BIOME_APPLIER;
	public static IslandEntityTracker ENTITY_TRACKER;
	public static DimensionRegistry DIMENSION_REGISTRY;
	public static FirstJoinTracker FIRST_JOIN_TRACKER;
	public static StarterKitConfig STARTER_KIT_CONFIG;
	public static RtpConfig RTP_CONFIG;
	public static SpawnConfig SPAWN_CONFIG;
	public static PortalLinkConfig PORTAL_LINK_CONFIG;
	public static VanillaResetConfig VANILLA_RESET_CONFIG;
	public static VanillaResetService VANILLA_RESET_SERVICE;
	public static FarmingConfig FARMING_CONFIG;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		// Must run before anything else: this is the earliest point Fabric guarantees mod code
		// runs (before server.properties/level.dat are even read this boot — see Sprint 18
		// research), so it's the only safe moment to apply a vanilla dimension reset requested
		// during the previous run.
		VanillaResetExecutor.executeIfPending();

		ISLAND_REGISTRY = new IslandRegistryImpl();
		ACCESS_CONTROLLER = new AccessControllerImpl();
		INVITE_MANAGER = new InviteManagerImpl();
		BIOME_TIER_REGISTRY = new BiomeTierRegistryImpl();
		BIOME_APPLIER = new IslandBiomeApplier();
		ENTITY_TRACKER = new IslandEntityTrackerImpl();
		DIMENSION_REGISTRY = new DimensionRegistryImpl(new FantasyDimensionRuntimeProvider(), new VanillaTeleportBackend());
		FIRST_JOIN_TRACKER = new FirstJoinTracker();
		STARTER_KIT_CONFIG = new StarterKitConfig();
		RTP_CONFIG = new RtpConfig();
		SPAWN_CONFIG = new SpawnConfig();
		PORTAL_LINK_CONFIG = new PortalLinkConfig();
		VANILLA_RESET_CONFIG = new VanillaResetConfig();
		VANILLA_RESET_SERVICE = new VanillaResetService(new VanillaTeleportBackend());
		ServerTickEvents.END_SERVER_TICK.register(server -> VANILLA_RESET_SERVICE.tickAll());
		FARMING_CONFIG = new FarmingConfig();
		ProtectionListeners.register();
		IslandCommand.register();
		DimensionCommand.register();
		RtpCommand.register();
		SpawnCommand.register();
		FarmingCommand.register();

		ServerPacketHandlers.register();
		ClientSyncNotifier.register();

		if (FabricLoader.getInstance().isModLoaded("luckperms")) {
			PERMISSION_PROVIDER = new LuckPermsProvider();
			LOGGER.info("LuckPerms detected: using LuckPermsProvider for permission checks.");
		} else {
			PERMISSION_PROVIDER = new FallbackPermissionProvider();
			LOGGER.info("LuckPerms not found: using FallbackPermissionProvider (permissions default to false).");
		}

		// Extra safety net on shutdown; individual mutations already persist themselves.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> ISLAND_REGISTRY.saveAll());

		TELEPORT_MANAGER = new TeleportManagerImpl(new VanillaTeleportBackend());
		ServerTickEvents.END_SERVER_TICK.register(server -> TELEPORT_MANAGER.tickAll());

		// Must be assigned before the server actually starts: IslandRegistryImpl.initializeStorage()
		// (triggered by SERVER_STARTED) calls DELETION_SERVICE.executeDeletion() to resume any
		// deletion interrupted by a previous shutdown. onInitialize() finishes well before that fires.
		DELETION_SERVICE = new IslandDeletionServiceImpl(new VanillaTeleportBackend());
		ServerTickEvents.END_SERVER_TICK.register(server -> DELETION_SERVICE.tickAll());
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayerEntity player) {
				TELEPORT_MANAGER.cancelPendingTeleport(player.getUuid(), "has recibido daño");
			}
			return true;
		});
		// A second, independent listener on the same event: Fabric ANDs every registered
		// AllowDamage callback together (any false denies the damage), so this doesn't need to
		// be merged with the teleport-cancellation listener above.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				DamageProtectionListener.isDamageAllowed(entity.getWorld(), entity, source));

		// Welcome teleport for brand-new players by default; reconnecting players keep vanilla's
		// normal "reappear where you left off" behavior UNLESS SpawnConfig.alwaysRespawnOnDisconnect
		// is true, in which case every connection (not just the first) gets teleported, but only the
		// actual first join ever gets the welcome message. Silently does nothing if the Spawn island
		// hasn't been created yet (no /island admin spawn create run) — vanilla Overworld spawn is
		// fine then. isFirstJoin() is called unconditionally: it also records the player as known,
		// which must happen on every join regardless of the flag below.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			boolean firstJoin = FIRST_JOIN_TRACKER.isFirstJoin(player.getUuid());

			// Independent of the welcome teleport below (which silently no-ops without a Spawn
			// island): the starter kit is about the player, not the island system, so it's given
			// on every actual first join regardless of whether a Spawn island exists yet.
			if (firstJoin) {
				giveStarterKit(player);
			}

			if (!firstJoin && !SPAWN_CONFIG.isAlwaysRespawnOnDisconnect()) {
				return;
			}

			ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID).ifPresent(spawnIsland -> {
				ServerWorld world = server.getWorld(spawnIsland.getDimension());
				if (world == null) {
					return;
				}

				boolean teleported = new VanillaTeleportBackend().teleport(player, world, spawnIsland.getHomeLocation());
				if (teleported && firstJoin) {
					player.sendMessage(Text.literal("¡Bienvenido a SkyFramework! Usa /island create para crear tu propia isla."), false);
				}
			});
		});

		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	// Skips (with a log warning) any entry whose item id doesn't resolve, rather than failing the
	// whole welcome flow over one bad config entry.
	private static void giveStarterKit(ServerPlayerEntity player) {
		for (StarterKitConfig.ItemStackDefinition definition : STARTER_KIT_CONFIG.getItems()) {
			Identifier itemId;
			try {
				itemId = Identifier.of(definition.item());
			} catch (RuntimeException e) {
				LOGGER.error("Skipping invalid starter kit item id: {}", definition.item(), e);
				continue;
			}

			Optional<Item> maybeItem = Registries.ITEM.getOrEmpty(itemId);
			if (maybeItem.isEmpty()) {
				LOGGER.error("Skipping unknown starter kit item id: {}", definition.item());
				continue;
			}

			player.getInventory().insertStack(new ItemStack(maybeItem.get(), definition.count()));
		}
	}
}
