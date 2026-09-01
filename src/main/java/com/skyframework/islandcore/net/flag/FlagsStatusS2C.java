package com.skyframework.islandcore.net.flag;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Sent only when the requesting player has an island — if not, the server sends
 * {@code ActionResultS2C.fail(ActionReason.NO_ISLAND)} instead (see FlagsStatusRequestC2S's
 * handler), no empty/placeholder snapshot. Wire field order: {@code flags} (list of
 * {@link FlagEntry}, in {@code FlagRegistry.all()}'s registration order — construccion, interact,
 * containers, entities, redstone, fire_spread, pvp_damage, mob_damage).
 */
public record FlagsStatusS2C(List<FlagEntry> flags) implements CustomPayload {

	public static final CustomPayload.Id<FlagsStatusS2C> ID = new CustomPayload.Id<>(NetworkChannels.FLAGS_STATUS_S2C);

	private static final PacketCodec<RegistryByteBuf, List<FlagEntry>> FLAG_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, FlagEntry.CODEC);

	public static final PacketCodec<RegistryByteBuf, FlagsStatusS2C> CODEC = PacketCodec.tuple(
			FLAG_LIST_CODEC, FlagsStatusS2C::flags,
			FlagsStatusS2C::new
	);

	@Override
	public CustomPayload.Id<FlagsStatusS2C> getId() {
		return ID;
	}

	/**
	 * Wire field order: {@code flagId}, {@code category} ({@code "ROLE_BASED"} or
	 * {@code "ISLAND_GLOBAL"}, {@link com.skyframework.islandcore.protection.flag.FlagCategory#name()}),
	 * {@code resolvedValue} (ISLAND_GLOBAL only — {@code FlagResolver#resolveGlobal}; always
	 * {@code false} and meaningless for a ROLE_BASED entry, read {@code resolvedByRole} instead),
	 * {@code resolvedByRole} (ROLE_BASED only — one {@link RoleValueEntry} per
	 * {@code IslandRole}, exactly what "/island flags" itself computes via
	 * {@code FlagResolver#resolveForRole}; always empty for an ISLAND_GLOBAL entry),
	 * {@code islandOverride} (this island's own override for the flag, {@link
	 * com.skyframework.islandcore.protection.flag.TriState#name()} — {@code "DEFAULT"} if never
	 * touched; for ROLE_BASED this is a single value since {@code /island flags set} always
	 * overrides every role uniformly, see {@code IslandData#setRoleFlagOverrideForAllRoles}),
	 * {@code currentPreset} (ROLE_BASED only, added after {@code islandOverride} — the
	 * {@link com.skyframework.islandcore.protection.flag.FlagPreset#getId()} whose VISITOR/ALLY/
	 * MEMBER/TRUSTED combination exactly matches this flag's current {@code resolvedByRole} values,
	 * or {@code "custom"} if none match; always {@code ""} — not applicable — for an ISLAND_GLOBAL
	 * entry).
	 */
	public record FlagEntry(
			String flagId,
			String category,
			boolean resolvedValue,
			List<RoleValueEntry> resolvedByRole,
			String islandOverride,
			String currentPreset
	) {
		private static final PacketCodec<RegistryByteBuf, List<RoleValueEntry>> ROLE_VALUE_LIST_CODEC =
				PacketCodecs.collection(ArrayList::new, RoleValueEntry.CODEC);

		public static final PacketCodec<RegistryByteBuf, FlagEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, FlagEntry::flagId,
				PacketCodecs.STRING, FlagEntry::category,
				PacketCodecs.BOOL, FlagEntry::resolvedValue,
				ROLE_VALUE_LIST_CODEC, FlagEntry::resolvedByRole,
				PacketCodecs.STRING, FlagEntry::islandOverride,
				PacketCodecs.STRING, FlagEntry::currentPreset,
				FlagEntry::new
		);
	}

	// Wire field order: role (IslandRole#name()), value (TriState#name() — always ALLOW or DENY,
	// FlagResolver#resolveForRole never returns DEFAULT).
	public record RoleValueEntry(String role, String value) {
		public static final PacketCodec<RegistryByteBuf, RoleValueEntry> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, RoleValueEntry::role,
				PacketCodecs.STRING, RoleValueEntry::value,
				RoleValueEntry::new
		);
	}
}
