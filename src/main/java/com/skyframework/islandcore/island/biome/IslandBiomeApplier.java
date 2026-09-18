package com.skyframework.islandcore.island.biome;

import com.skyframework.islandcore.island.model.IslandBounds;
import com.skyframework.islandcore.util.ServerLang;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

// Sets the real Minecraft biome over an island's plot, chunk column by chunk column, budgeted
// across ticks the same way IslandDeletionServiceImpl budgets its block-clearing. Reuses the
// exact mechanism vanilla's own /fillbiome command uses (Chunk.populateBiomes + a BiomeSupplier),
// confirmed by decompiling FillBiomeCommand: no Mixin needed. On top of what vanilla does, each
// touched chunk is also resent as a full ChunkDataS2CPacket to every player currently watching
// it, so grass/foliage/water color refreshes immediately instead of needing a manual F3+A.
public class IslandBiomeApplier {

	// Adjustable: how many chunk columns to process per server tick.
	private static final int CHUNKS_PER_TICK = 3;

	private final Deque<PendingBiomeJob> jobs = new ArrayDeque<>();

	// onInitialize() runs before the MinecraftServer exists, so the reference is captured lazily on server start.
	private MinecraftServer server;

	public IslandBiomeApplier() {
		ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> this.server = startedServer);
		ServerTickEvents.END_SERVER_TICK.register(server -> tick());
	}

	public void enqueue(ServerWorld world, IslandBounds bounds, RegistryEntry<Biome> targetBiome, UUID requestedBy) {
		jobs.add(new PendingBiomeJob(world, bounds, targetBiome, requestedBy, true, null));
	}

	// Used by IslandDeletionServiceImpl to reset a deleted island's plot back to void: notifyPlayer
	// is false there (the "your biome change finished" message would be confusing when the island
	// is being deleted, not changed), and onFinished lets the deletion service chain its final
	// cleanup (spatial index/grid/storage) only once the plot's biome has actually been reset —
	// same tick-budget/chunk-resend mechanism as the 4-arg overload above, nothing duplicated.
	public void enqueue(ServerWorld world, IslandBounds bounds, RegistryEntry<Biome> targetBiome, UUID requestedBy,
			boolean notifyPlayer, Runnable onFinished) {
		jobs.add(new PendingBiomeJob(world, bounds, targetBiome, requestedBy, notifyPlayer, onFinished));
	}

	private void tick() {
		int budget = CHUNKS_PER_TICK;

		while (budget > 0) {
			PendingBiomeJob job = jobs.peek();
			if (job == null) {
				return;
			}

			while (budget > 0 && !job.remainingColumns.isEmpty()) {
				processColumn(job, job.remainingColumns.poll());
				budget--;
			}

			if (job.remainingColumns.isEmpty()) {
				jobs.poll();
				if (job.notifyPlayer) {
					notifyFinished(job);
				}
				if (job.onFinished != null) {
					job.onFinished.run();
				}
			}
		}
	}

	private void processColumn(PendingBiomeJob job, ChunkPos chunkPos) {
		WorldChunk chunk = job.world.getChunk(chunkPos.x, chunkPos.z);

		BiomeSupplier supplier = (biomeX, biomeY, biomeZ, sampler) -> {
			BlockPos blockPos = new BlockPos(BiomeCoords.toBlock(biomeX), BiomeCoords.toBlock(biomeY), BiomeCoords.toBlock(biomeZ));
			if (job.bounds.contains(blockPos)) {
				return job.targetBiome;
			}
			return chunk.getBiomeForNoiseGen(biomeX, biomeY, biomeZ);
		};

		chunk.populateBiomes(supplier, job.noiseSampler);
		chunk.setNeedsSaving(true);

		ServerChunkLoadingManager chunkLoadingManager = job.world.getChunkManager().chunkLoadingManager;
		chunkLoadingManager.sendChunkBiomePackets(List.of(chunk));

		ChunkDataS2CPacket fullChunkPacket = new ChunkDataS2CPacket(chunk, job.world.getLightingProvider(), null, null);
		for (ServerPlayerEntity watcher : chunkLoadingManager.getPlayersWatchingChunk(chunkPos)) {
			watcher.networkHandler.sendPacket(fullChunkPacket);
		}
	}

	private void notifyFinished(PendingBiomeJob job) {
		if (server == null) {
			return;
		}

		ServerPlayerEntity player = server.getPlayerManager().getPlayer(job.requestedBy);
		if (player != null) {
			player.sendMessage(ServerLang.of(player,
					"El cambio de bioma de tu isla ha terminado.", "Your island's biome change has finished."), false);
		}
	}

	private static Deque<ChunkPos> collectColumns(IslandBounds bounds) {
		int minChunkX = ChunkSectionPos.getSectionCoord(bounds.min().getX());
		int maxChunkX = ChunkSectionPos.getSectionCoord(bounds.max().getX());
		int minChunkZ = ChunkSectionPos.getSectionCoord(bounds.min().getZ());
		int maxChunkZ = ChunkSectionPos.getSectionCoord(bounds.max().getZ());

		Deque<ChunkPos> columns = new ArrayDeque<>();
		for (int x = minChunkX; x <= maxChunkX; x++) {
			for (int z = minChunkZ; z <= maxChunkZ; z++) {
				columns.add(new ChunkPos(x, z));
			}
		}
		return columns;
	}

	private static final class PendingBiomeJob {
		final ServerWorld world;
		final IslandBounds bounds;
		final RegistryEntry<Biome> targetBiome;
		final UUID requestedBy;
		final boolean notifyPlayer;
		final Runnable onFinished;
		final MultiNoiseUtil.MultiNoiseSampler noiseSampler;
		final Deque<ChunkPos> remainingColumns;

		PendingBiomeJob(ServerWorld world, IslandBounds bounds, RegistryEntry<Biome> targetBiome, UUID requestedBy,
				boolean notifyPlayer, Runnable onFinished) {
			this.world = world;
			this.bounds = bounds;
			this.targetBiome = targetBiome;
			this.requestedBy = requestedBy;
			this.notifyPlayer = notifyPlayer;
			this.onFinished = onFinished;
			this.noiseSampler = world.getChunkManager().getNoiseConfig().getMultiNoiseSampler();
			this.remainingColumns = collectColumns(bounds);
		}
	}
}
