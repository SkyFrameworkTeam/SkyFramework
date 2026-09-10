package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Wire field order: {@code enabled} (the Spawn island's current BUILD_PROTECTION IslandSetting
 * value; {@code false} if the Spawn island doesn't exist yet — matches BUILD_PROTECTION's own
 * default), {@code authorizedPlayers} (list of {@link AuthorizedPlayerEntry}: the Spawn island's
 * MEMBER/CO_OWNER members — the same "real members" filter {@link AdminIslandBuilder}/
 * {@code IslandSnapshotBuilder} already use elsewhere — who can always build there regardless of
 * {@code enabled}, empty if the Spawn island doesn't exist).
 */
public record SpawnBuildProtectionStatusS2C(boolean enabled, List<AuthorizedPlayerEntry> authorizedPlayers) implements CustomPayload {

	public static final CustomPayload.Id<SpawnBuildProtectionStatusS2C> ID =
			new CustomPayload.Id<>(NetworkChannels.SPAWN_BUILD_PROTECTION_STATUS_S2C);

	private static final PacketCodec<RegistryByteBuf, List<AuthorizedPlayerEntry>> AUTHORIZED_PLAYERS_CODEC =
			PacketCodecs.collection(ArrayList::new, AuthorizedPlayerEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, SpawnBuildProtectionStatusS2C> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL, SpawnBuildProtectionStatusS2C::enabled,
			AUTHORIZED_PLAYERS_CODEC, SpawnBuildProtectionStatusS2C::authorizedPlayers,
			SpawnBuildProtectionStatusS2C::new
	);

	public static SpawnBuildProtectionStatusS2C absent() {
		return new SpawnBuildProtectionStatusS2C(false, List.of());
	}

	@Override
	public CustomPayload.Id<SpawnBuildProtectionStatusS2C> getId() {
		return ID;
	}

	public record AuthorizedPlayerEntry(UUID uuid, String name, String role) {
		public static final PacketCodec<RegistryByteBuf, AuthorizedPlayerEntry> CODEC = PacketCodec.tuple(
				Uuids.PACKET_CODEC, AuthorizedPlayerEntry::uuid,
				PacketCodecs.STRING, AuthorizedPlayerEntry::name,
				PacketCodecs.STRING, AuthorizedPlayerEntry::role,
				AuthorizedPlayerEntry::new
		);
	}
}
