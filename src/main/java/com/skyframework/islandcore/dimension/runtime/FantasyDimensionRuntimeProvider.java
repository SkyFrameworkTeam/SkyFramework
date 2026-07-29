package com.skyframework.islandcore.dimension.runtime;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.dimension.model.DimensionDefinition;
import com.skyframework.islandcore.dimension.model.DimensionGeneratorStyle;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.biome.source.TheEndBiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.FlatChunkGenerator;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;

import java.util.List;
import java.util.Optional;

import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

/*
 * Sprint 16, Parte 1 — investigation notes on vanilla generator wiring.
 *
 * As of 1.21.1, the game jar does NOT ship data/minecraft/dimension/*.json for the vanilla
 * dimensions (confirmed: extracted the merged jar, only data/minecraft/dimension_type/*.json is
 * present — the combined "dimension.json" recipe files simply don't exist there; Overworld/Nether/
 * End are registered from Java bootstrap code instead). So this was confirmed by reading the
 * dimension_type JSONs plus the ChunkGeneratorSettings/BiomeSource classes directly:
 *
 * - Overworld: generator type "minecraft:noise", ChunkGeneratorSettings.OVERWORLD, biome source =
 *   MultiNoiseBiomeSource with preset "minecraft:overworld"
 *   (data/minecraft/worldgen/multi_noise_biome_source_parameter_list/overworld.json = {"preset":"minecraft:overworld"}).
 * - Nether: generator type "minecraft:noise", ChunkGeneratorSettings.NETHER, biome source =
 *   MultiNoiseBiomeSource with preset "minecraft:nether" (.../nether.json = {"preset":"minecraft:nether"}).
 * - End: generator type "minecraft:noise", ChunkGeneratorSettings.END — but its biome source is
 *   NOT multi_noise at all (no end.json under multi_noise_biome_source_parameter_list). The End
 *   uses the dedicated TheEndBiomeSource.createVanilla(biomeLookup): a fixed 5-biome
 *   (center/highlands/midlands/small_islands/barrens) source positioned purely by distance from
 *   the origin, unrelated to noise sampling.
 * - Our own islandcore:islands (data/islandcore/dimension/islands.json): generator type
 *   "minecraft:flat", biome "minecraft:the_void", empty layers, lakes/features disabled. VOID_FLAT
 *   below reproduces exactly this in Java via FlatChunkGenerator + FlatChunkGeneratorConfig.
 */
public class FantasyDimensionRuntimeProvider implements DimensionRuntimeProvider {

	@Override
	public ServerWorld createOrLoadWorld(DimensionDefinition definition, MinecraftServer server) {
		RuntimeWorldConfig config = new RuntimeWorldConfig()
				.setSeed(definition.getSeed())
				.setGenerator(createChunkGenerator(definition.getGeneratorStyle(), server));

		RuntimeWorldHandle handle = Fantasy.get(server).getOrOpenPersistentWorld(definition.getId(), config);
		return handle.asWorld();
	}

	@Override
	public void unloadAndDeleteWorld(Identifier id, MinecraftServer server) {
		RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, id);
		if (server.getWorld(worldKey) == null) {
			// Not currently loaded: nothing to delete right now. Passing a null config into
			// getOrOpenPersistentWorld here would try to (re)create it first — a real bug we
			// found in Multiworld's own delete command (GitHub issue #143) — so this bails out
			// instead of risking the same NullPointerException.
			IslandCoreMod.LOGGER.warn("unloadAndDeleteWorld: dimension {} isn't currently loaded, skipping", id);
			return;
		}

		RuntimeWorldHandle handle = Fantasy.get(server).getOrOpenPersistentWorld(id, null);
		handle.delete();
	}

	private static ChunkGenerator createChunkGenerator(DimensionGeneratorStyle style, MinecraftServer server) {
		Registry<Biome> biomeRegistry = server.getRegistryManager().get(RegistryKeys.BIOME);

		return switch (style) {
			case OVERWORLD_LIKE -> new NoiseChunkGenerator(
					multiNoiseBiomeSource(server, "overworld"),
					chunkGeneratorSettings(server, ChunkGeneratorSettings.OVERWORLD));
			case NETHER_LIKE -> new NoiseChunkGenerator(
					multiNoiseBiomeSource(server, "nether"),
					chunkGeneratorSettings(server, ChunkGeneratorSettings.NETHER));
			case END_LIKE -> new NoiseChunkGenerator(
					TheEndBiomeSource.createVanilla(server.getRegistryManager().getWrapperOrThrow(RegistryKeys.BIOME)),
					chunkGeneratorSettings(server, ChunkGeneratorSettings.END));
			case VOID_FLAT -> new FlatChunkGenerator(new FlatChunkGeneratorConfig(
					Optional.empty(),
					biomeRegistry.getEntry(biomeRegistry.getOrThrow(BiomeKeys.THE_VOID)),
					List.of()));
		};
	}

	private static BiomeSource multiNoiseBiomeSource(MinecraftServer server, String vanillaPresetPath) {
		Registry<MultiNoiseBiomeSourceParameterList> presets =
				server.getRegistryManager().get(RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
		RegistryKey<MultiNoiseBiomeSourceParameterList> presetKey = RegistryKey.of(
				RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, Identifier.of("minecraft", vanillaPresetPath));

		RegistryEntry<MultiNoiseBiomeSourceParameterList> preset = presets.getEntry(presetKey)
				.orElseThrow(() -> new IllegalStateException("Missing vanilla multi-noise preset: " + vanillaPresetPath));

		return MultiNoiseBiomeSource.create(preset);
	}

	private static RegistryEntry<ChunkGeneratorSettings> chunkGeneratorSettings(
			MinecraftServer server, RegistryKey<ChunkGeneratorSettings> key) {
		return server.getRegistryManager().get(RegistryKeys.CHUNK_GENERATOR_SETTINGS)
				.getEntry(key)
				.orElseThrow(() -> new IllegalStateException("Missing vanilla chunk generator settings: " + key.getValue()));
	}
}
