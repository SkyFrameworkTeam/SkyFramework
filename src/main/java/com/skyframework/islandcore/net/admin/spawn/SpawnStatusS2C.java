package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Wire field order: {@code exists}, {@code size} (0 if {@code exists} is false),
 * {@code homeLocation} (empty if {@code exists} is false). {@code exists = false} is a normal,
 * expected state (the Spawn island simply hasn't been created yet) — not an error, so this is
 * never wrapped in an {@code ActionResultS2C} failure.
 */
public record SpawnStatusS2C(boolean exists, int size, Optional<BlockPos> homeLocation) implements CustomPayload {

	public static final CustomPayload.Id<SpawnStatusS2C> ID = new CustomPayload.Id<>(NetworkChannels.SPAWN_STATUS_S2C);

	// See IslandSnapshotS2C's HOME_CODEC comment: PacketCodecs.optional needs an exact ByteBuf
	// match, so this stays typed over plain ByteBuf rather than RegistryByteBuf.
	private static final PacketCodec<ByteBuf, Optional<BlockPos>> HOME_CODEC = PacketCodecs.optional(BlockPos.PACKET_CODEC);

	public static final PacketCodec<RegistryByteBuf, SpawnStatusS2C> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL, SpawnStatusS2C::exists,
			PacketCodecs.VAR_INT, SpawnStatusS2C::size,
			HOME_CODEC, SpawnStatusS2C::homeLocation,
			SpawnStatusS2C::new
	);

	public static SpawnStatusS2C absent() {
		return new SpawnStatusS2C(false, 0, Optional.empty());
	}

	@Override
	public CustomPayload.Id<SpawnStatusS2C> getId() {
		return ID;
	}
}
