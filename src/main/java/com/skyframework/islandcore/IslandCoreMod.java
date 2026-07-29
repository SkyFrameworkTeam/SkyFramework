package com.skyframework.islandcore;

import com.skyframework.islandcore.api.permission.PermissionProvider;
import com.skyframework.islandcore.api.registry.IslandRegistryApi;
import com.skyframework.islandcore.command.DimensionCommand;
import com.skyframework.islandcore.command.IslandCommand;
import com.skyframework.islandcore.dimension.registry.DimensionRegistry;
import com.skyframework.islandcore.dimension.registry.DimensionRegistryImpl;
import com.skyframework.islandcore.dimension.runtime.FantasyDimensionRuntimeProvider;
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
import com.skyframework.islandcore.permission.FallbackPermissionProvider;
import com.skyframework.islandcore.permission.LuckPermsProvider;
import com.skyframework.islandcore.protection.AccessController;
import com.skyframework.islandcore.protection.AccessControllerImpl;
import com.skyframework.islandcore.protection.DamageProtectionListener;
import com.skyframework.islandcore.protection.ProtectionListeners;
import com.skyframework.islandcore.teleport.TeleportManager;
import com.skyframework.islandcore.teleport.TeleportManagerImpl;
import com.skyframework.islandcore.teleport.VanillaTeleportBackend;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ISLAND_REGISTRY = new IslandRegistryImpl();
		ACCESS_CONTROLLER = new AccessControllerImpl();
		INVITE_MANAGER = new InviteManagerImpl();
		BIOME_TIER_REGISTRY = new BiomeTierRegistryImpl();
		BIOME_APPLIER = new IslandBiomeApplier();
		ENTITY_TRACKER = new IslandEntityTrackerImpl();
		DIMENSION_REGISTRY = new DimensionRegistryImpl(new FantasyDimensionRuntimeProvider());
		ProtectionListeners.register();
		IslandCommand.register();
		DimensionCommand.register();

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

		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
