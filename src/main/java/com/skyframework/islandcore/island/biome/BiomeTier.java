package com.skyframework.islandcore.island.biome;

import net.minecraft.util.Identifier;

import java.util.Set;

// permission == null means this is a base tier: any player can use its biomes with no permission check.
public record BiomeTier(String id, String permission, Set<Identifier> biomes) {
}
