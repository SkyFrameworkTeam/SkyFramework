package com.skyframework.islandcore.net.admin.defaults;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * The server-wide default configuration for every ROLE_BASED flag and every exception group — the
 * "servidor" layer in FlagResolver/ExceptionResolver's resolution chain, not any specific island.
 * Scoped to preset-shaped entries only: the 3 ISLAND_GLOBAL flags (fire_spread/pvp_damage/
 * mob_damage) have no preset concept and keep their admin-facing allow/deny default
 * command-only ("/island admin flags set-default <flag> allow|deny") — see IslandCommand.
 *
 * <p>Wire field order: {@code flagDefaults} (list of {@link FlagDefaultEntry}, in
 * {@code FlagRegistry.all()}'s registration order, ROLE_BASED flags only), {@code exceptionDefaults}
 * (list of {@link ExceptionDefaultEntry}, in {@code ExceptionGroupRegistry.getAllGroups()}'s
 * registration order).
 */
public record AdminDefaultsStatusS2C(List<FlagDefaultEntry> flagDefaults, List<ExceptionDefaultEntry> exceptionDefaults) implements CustomPayload {

	public static final CustomPayload.Id<AdminDefaultsStatusS2C> ID =
			new CustomPayload.Id<>(NetworkChannels.ADMIN_DEFAULTS_STATUS_S2C);

	private static final PacketCodec<RegistryByteBuf, List<FlagDefaultEntry>> FLAG_DEFAULT_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, FlagDefaultEntry.CODEC);
	private static final PacketCodec<RegistryByteBuf, List<ExceptionDefaultEntry>> EXCEPTION_DEFAULT_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, ExceptionDefaultEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, AdminDefaultsStatusS2C> CODEC = PacketCodec.tuple(
			FLAG_DEFAULT_LIST_CODEC, AdminDefaultsStatusS2C::flagDefaults,
			EXCEPTION_DEFAULT_LIST_CODEC, AdminDefaultsStatusS2C::exceptionDefaults,
			AdminDefaultsStatusS2C::new
	);

	@Override
	public CustomPayload.Id<AdminDefaultsStatusS2C> getId() {
		return ID;
	}

	// currentPreset: "nadie"/"miembros"/"aliados"/"todos" if the server default (or, absent that,
	// the flag's own hardcoded table) exactly matches one of those 4 combinations for
	// VISITOR/ALLY/MEMBER, or "custom" if not — see FlagResolver#resolveServerDefaultForRole
	// and FlagPreset#matching.
	public record FlagDefaultEntry(String flagId, String currentPreset) {
		public static final PacketCodec<RegistryByteBuf, FlagDefaultEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, FlagDefaultEntry::flagId,
				PacketCodecs.STRING, FlagDefaultEntry::currentPreset,
				FlagDefaultEntry::new
		);
	}

	// currentPreset: same meaning as FlagDefaultEntry#currentPreset, but for an exception group —
	// see ExceptionResolver#isEnabledForRoleServerDefault.
	public record ExceptionDefaultEntry(String groupId, String currentPreset) {
		public static final PacketCodec<RegistryByteBuf, ExceptionDefaultEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, ExceptionDefaultEntry::groupId,
				PacketCodecs.STRING, ExceptionDefaultEntry::currentPreset,
				ExceptionDefaultEntry::new
		);
	}
}
