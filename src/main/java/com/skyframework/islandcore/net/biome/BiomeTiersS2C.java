package com.skyframework.islandcore.net.biome;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Every configured biome tier (built by BiomeTiersBuilder from BiomeTierRegistry#getAllTiers,
// reusing #canUse per tier for the "unlocked" flag), so the client can render locked tiers too
// (e.g. "unlocks with permission X") instead of only ever seeing biomes it already has access to.
public record BiomeTiersS2C(List<TierEntry> tiers) implements CustomPayload {

	public static final CustomPayload.Id<BiomeTiersS2C> ID = new CustomPayload.Id<>(NetworkChannels.BIOME_TIERS_S2C);

	private static final PacketCodec<RegistryByteBuf, List<TierEntry>> TIERS_CODEC =
			PacketCodecs.collection(ArrayList::new, TierEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, BiomeTiersS2C> CODEC = PacketCodec.tuple(
			TIERS_CODEC, BiomeTiersS2C::tiers,
			BiomeTiersS2C::new
	);

	@Override
	public CustomPayload.Id<BiomeTiersS2C> getId() {
		return ID;
	}

	// permissionRequired is empty for a base tier (BiomeTier#permission() == null), matching
	// BiomeTierRegistry's own "null permission == usable by anyone" convention.
	public record TierEntry(String tierId, Optional<String> permissionRequired, boolean unlocked, List<BiomeEntry> biomes) {

		private static final PacketCodec<ByteBuf, Optional<String>> PERMISSION_CODEC = PacketCodecs.optional(PacketCodecs.STRING);
		private static final PacketCodec<RegistryByteBuf, List<BiomeEntry>> BIOMES_CODEC =
				PacketCodecs.collection(ArrayList::new, BiomeEntry.CODEC);

		public static final PacketCodec<RegistryByteBuf, TierEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, TierEntry::tierId,
				PERMISSION_CODEC, TierEntry::permissionRequired,
				PacketCodecs.BOOL, TierEntry::unlocked,
				BIOMES_CODEC, TierEntry::biomes,
				TierEntry::new
		);
	}

	// label is a display-friendly derivation of the biome's Identifier path (e.g.
	// "minecraft:dark_forest" -> "Dark Forest") computed once server-side by BiomeTiersBuilder, so
	// every client renders the same text without each one reimplementing the same formatting.
	public record BiomeEntry(String biomeId, String label) {
		public static final PacketCodec<RegistryByteBuf, BiomeEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, BiomeEntry::biomeId,
				PacketCodecs.STRING, BiomeEntry::label,
				BiomeEntry::new
		);
	}
}
