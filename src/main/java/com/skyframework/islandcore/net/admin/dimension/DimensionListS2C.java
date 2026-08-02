package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Every dimension {@code DimensionRegistry} manages, same fields "/dimension list" prints per
 * line. Wire field order: {@code dimensions} (list of {@link DimensionEntry}).
 */
public record DimensionListS2C(List<DimensionEntry> dimensions) implements CustomPayload {

	public static final CustomPayload.Id<DimensionListS2C> ID = new CustomPayload.Id<>(NetworkChannels.DIMENSION_LIST_S2C);

	private static final PacketCodec<RegistryByteBuf, List<DimensionEntry>> DIMENSION_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, DimensionEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, DimensionListS2C> CODEC = PacketCodec.tuple(
			DIMENSION_LIST_CODEC, DimensionListS2C::dimensions,
			DimensionListS2C::new
	);

	@Override
	public CustomPayload.Id<DimensionListS2C> getId() {
		return ID;
	}

	/**
	 * Wire field order: {@code id} (full Identifier as a String, e.g. {@code "islandcore:foo"}),
	 * {@code displayName}, {@code style} ({@link com.skyframework.islandcore.dimension.model.DimensionGeneratorStyle}
	 * name), {@code seed}, {@code state} ({@link com.skyframework.islandcore.dimension.model.DimensionState} name).
	 */
	public record DimensionEntry(String id, String displayName, String style, long seed, String state) {
		public static final PacketCodec<RegistryByteBuf, DimensionEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, DimensionEntry::id,
				PacketCodecs.STRING, DimensionEntry::displayName,
				PacketCodecs.STRING, DimensionEntry::style,
				PacketCodecs.VAR_LONG, DimensionEntry::seed,
				PacketCodecs.STRING, DimensionEntry::state,
				DimensionEntry::new
		);
	}
}
