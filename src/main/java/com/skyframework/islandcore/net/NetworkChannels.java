package com.skyframework.islandcore.net;

import net.minecraft.util.Identifier;

// Single source of truth for every "islandcore" custom payload channel. Handshake identifiers
// here MUST match IslandCoreClient's network.handshake.ClientHandshakeC2S/ServerHandshakeS2C
// exactly (that mod hardcodes them directly, with no shared module between the two projects) —
// this is the reason both sides live under the "islandcore" namespace rather than
// "islandcoreclient": the server owns this protocol.
public final class NetworkChannels {

	// Bump this by 1 every time the binary format of ANY existing payload in this package changes —
	// a field added/removed/reordered, a type swapped, anything that changes what bytes go on the
	// wire (e.g. IslandSnapshotS2C's 11 -> 13 field change). Do it in the SAME change that alters
	// the format, not as an afterthought. ClientHandshakeC2S/ServerHandshakeS2C carry this value so
	// each side can tell whether the other was built against the wire format it expects — see
	// ServerHandshakeS2C#protocolCompatible. Bumping it server-side only helps if the client's own
	// mirror constant (network.handshake package there) is bumped to match in the same release;
	// announce that in the client's chat/changelog whenever this changes, since nothing here can
	// notify that codebase automatically.
	//
	// Exception: this constant cannot protect ClientHandshakeC2S/ServerHandshakeS2C's OWN format
	// (that would require decoding the payload before knowing whether it's safe to decode it) — a
	// change to the handshake payloads themselves still requires shipping server and client
	// together, the same as every codec change in this project always has.
	public static final int PROTOCOL_VERSION = 4;

	public static final Identifier HANDSHAKE_C2S = Identifier.of("islandcore", "handshake_c2s");
	public static final Identifier HANDSHAKE_S2C = Identifier.of("islandcore", "handshake_s2c");

	public static final Identifier ISLAND_SNAPSHOT_REQUEST_C2S = Identifier.of("islandcore", "island_snapshot_request_c2s");
	public static final Identifier ISLAND_SNAPSHOT_S2C = Identifier.of("islandcore", "island_snapshot_s2c");

	// Sprint "acciones de isla":

	public static final Identifier ACTION_RESULT_S2C = Identifier.of("islandcore", "action_result_s2c");
	public static final Identifier PENDING_CONFIRMATION_TICK_S2C = Identifier.of("islandcore", "pending_confirmation_tick_s2c");

	public static final Identifier ISLAND_CREATE_C2S = Identifier.of("islandcore", "island_create_c2s");
	public static final Identifier ISLAND_UPGRADE_C2S = Identifier.of("islandcore", "island_upgrade_c2s");
	public static final Identifier ISLAND_DELETE_REQUEST_C2S = Identifier.of("islandcore", "island_delete_request_c2s");
	public static final Identifier ISLAND_DELETE_CONFIRM_C2S = Identifier.of("islandcore", "island_delete_confirm_c2s");
	public static final Identifier ISLAND_SETTINGS_UPDATE_C2S = Identifier.of("islandcore", "island_settings_update_c2s");
	public static final Identifier ISLAND_BIOME_CHANGE_C2S = Identifier.of("islandcore", "island_biome_change_c2s");

	public static final Identifier MEMBER_INVITE_C2S = Identifier.of("islandcore", "member_invite_c2s");
	public static final Identifier MEMBER_INVITE_ACCEPT_C2S = Identifier.of("islandcore", "member_invite_accept_c2s");
	public static final Identifier MEMBER_INVITE_DECLINE_C2S = Identifier.of("islandcore", "member_invite_decline_c2s");
	public static final Identifier MEMBER_TRUST_C2S = Identifier.of("islandcore", "member_trust_c2s");
	public static final Identifier MEMBER_REMOVE_C2S = Identifier.of("islandcore", "member_remove_c2s");
	public static final Identifier MEMBER_ALLY_ADD_C2S = Identifier.of("islandcore", "member_ally_add_c2s");
	public static final Identifier MEMBER_ALLY_REMOVE_C2S = Identifier.of("islandcore", "member_ally_remove_c2s");

	public static final Identifier TELEPORT_REQUEST_C2S = Identifier.of("islandcore", "teleport_request_c2s");
	public static final Identifier TELEPORT_STATUS_REQUEST_C2S = Identifier.of("islandcore", "teleport_status_request_c2s");
	public static final Identifier TELEPORT_STATUS_S2C = Identifier.of("islandcore", "teleport_status_s2c");

	public static final Identifier BIOME_TIERS_REQUEST_C2S = Identifier.of("islandcore", "biome_tiers_request_c2s");
	public static final Identifier BIOME_TIERS_S2C = Identifier.of("islandcore", "biome_tiers_s2c");

	// Admin network block: island list/detail/delete, Spawn management, Dimension Manager, vanilla
	// reset queue. All require the sender to be a server operator (see ActionReason's admin block
	// comment for the "error." prefix reason keys these use on failure).

	public static final Identifier ADMIN_ISLAND_LIST_REQUEST_C2S = Identifier.of("islandcore", "admin_island_list_request_c2s");
	public static final Identifier ADMIN_ISLAND_LIST_S2C = Identifier.of("islandcore", "admin_island_list_s2c");
	public static final Identifier ADMIN_ISLAND_DETAIL_REQUEST_C2S = Identifier.of("islandcore", "admin_island_detail_request_c2s");
	public static final Identifier ADMIN_ISLAND_DETAIL_S2C = Identifier.of("islandcore", "admin_island_detail_s2c");
	public static final Identifier ADMIN_ISLAND_DELETE_C2S = Identifier.of("islandcore", "admin_island_delete_c2s");
	public static final Identifier ADMIN_ISLAND_DELETE_CONFIRM_C2S = Identifier.of("islandcore", "admin_island_delete_confirm_c2s");

	public static final Identifier SPAWN_STATUS_REQUEST_C2S = Identifier.of("islandcore", "spawn_status_request_c2s");
	public static final Identifier SPAWN_STATUS_S2C = Identifier.of("islandcore", "spawn_status_s2c");
	public static final Identifier SPAWN_ISLAND_CREATE_C2S = Identifier.of("islandcore", "spawn_island_create_c2s");
	public static final Identifier SPAWN_ISLAND_RESIZE_C2S = Identifier.of("islandcore", "spawn_island_resize_c2s");
	public static final Identifier SPAWN_ISLAND_SET_HOME_C2S = Identifier.of("islandcore", "spawn_island_set_home_c2s");

	// Spawn island configurable build protection (BUILD_PROTECTION IslandSetting) + its
	// always-authorized (CO_OWNER/MEMBER) player list.
	public static final Identifier SPAWN_BUILD_PROTECTION_STATUS_REQUEST_C2S =
			Identifier.of("islandcore", "spawn_build_protection_status_request_c2s");
	public static final Identifier SPAWN_BUILD_PROTECTION_STATUS_S2C =
			Identifier.of("islandcore", "spawn_build_protection_status_s2c");
	public static final Identifier SPAWN_BUILD_PROTECTION_SET_C2S = Identifier.of("islandcore", "spawn_build_protection_set_c2s");
	public static final Identifier SPAWN_AUTHORIZED_PLAYER_ADD_C2S = Identifier.of("islandcore", "spawn_authorized_player_add_c2s");
	public static final Identifier SPAWN_AUTHORIZED_PLAYER_REMOVE_C2S = Identifier.of("islandcore", "spawn_authorized_player_remove_c2s");

	public static final Identifier DIMENSION_LIST_REQUEST_C2S = Identifier.of("islandcore", "dimension_list_request_c2s");
	public static final Identifier DIMENSION_LIST_S2C = Identifier.of("islandcore", "dimension_list_s2c");
	public static final Identifier DIMENSION_DETAIL_REQUEST_C2S = Identifier.of("islandcore", "dimension_detail_request_c2s");
	public static final Identifier DIMENSION_DETAIL_S2C = Identifier.of("islandcore", "dimension_detail_s2c");
	public static final Identifier DIMENSION_CREATE_C2S = Identifier.of("islandcore", "dimension_create_c2s");
	public static final Identifier DIMENSION_DELETE_C2S = Identifier.of("islandcore", "dimension_delete_c2s");
	public static final Identifier DIMENSION_DELETE_CONFIRM_C2S = Identifier.of("islandcore", "dimension_delete_confirm_c2s");
	public static final Identifier DIMENSION_REGENERATE_C2S = Identifier.of("islandcore", "dimension_regenerate_c2s");
	public static final Identifier DIMENSION_REGENERATE_CONFIRM_C2S = Identifier.of("islandcore", "dimension_regenerate_confirm_c2s");

	public static final Identifier VANILLA_RESET_LIST_REQUEST_C2S = Identifier.of("islandcore", "vanilla_reset_list_request_c2s");
	public static final Identifier VANILLA_RESET_LIST_S2C = Identifier.of("islandcore", "vanilla_reset_list_s2c");
	public static final Identifier VANILLA_RESET_QUEUE_C2S = Identifier.of("islandcore", "vanilla_reset_queue_c2s");
	public static final Identifier VANILLA_RESET_CONFIRM_C2S = Identifier.of("islandcore", "vanilla_reset_confirm_c2s");
	public static final Identifier VANILLA_RESET_CANCEL_C2S = Identifier.of("islandcore", "vanilla_reset_cancel_c2s");

	// Flags + exception groups network block: player-facing (not admin-only), mirrors what
	// "/island flags"/"/island exceptions" already compute — see FlagsStatusBuilder.

	public static final Identifier FLAGS_STATUS_REQUEST_C2S = Identifier.of("islandcore", "flags_status_request_c2s");
	public static final Identifier FLAGS_STATUS_S2C = Identifier.of("islandcore", "flags_status_s2c");
	public static final Identifier FLAG_SET_C2S = Identifier.of("islandcore", "flag_set_c2s");
	public static final Identifier FLAG_SET_PRESET_C2S = Identifier.of("islandcore", "flag_set_preset_c2s");
	public static final Identifier EXCEPTION_GROUPS_STATUS_REQUEST_C2S = Identifier.of("islandcore", "exception_groups_status_request_c2s");
	public static final Identifier EXCEPTION_GROUPS_STATUS_S2C = Identifier.of("islandcore", "exception_groups_status_s2c");
	// Replaces the old boolean-shaped exception_group_set_c2s (Sprint "excepciones por rol") —
	// exception groups now resolve per role via a 4-way preset, exact mirror of FLAG_SET_PRESET_C2S.
	public static final Identifier EXCEPTION_GROUP_SET_PRESET_C2S = Identifier.of("islandcore", "exception_group_set_preset_c2s");

	// Admin-only: server-wide default configuration (not any specific island) for ROLE_BASED flags
	// and exception groups — the "servidor" layer in FlagResolver/ExceptionResolver's resolution
	// chain. Requires operator (see ServerPacketHandlers#rejectIfNotOperator), same as every other
	// admin network payload.
	public static final Identifier ADMIN_DEFAULTS_STATUS_REQUEST_C2S = Identifier.of("islandcore", "admin_defaults_status_request_c2s");
	public static final Identifier ADMIN_DEFAULTS_STATUS_S2C = Identifier.of("islandcore", "admin_defaults_status_s2c");
	public static final Identifier ADMIN_FLAG_SET_SERVER_DEFAULT_C2S = Identifier.of("islandcore", "admin_flag_set_server_default_c2s");
	public static final Identifier ADMIN_EXCEPTION_SET_SERVER_DEFAULT_C2S = Identifier.of("islandcore", "admin_exception_set_server_default_c2s");
	public static final Identifier ADMIN_FLAG_SET_REQUIREMENT_C2S = Identifier.of("islandcore", "admin_flag_set_requirement_c2s");

	// Party network block: mirrors "/party" — see PartyStatusBuilder.

	public static final Identifier PARTY_STATUS_REQUEST_C2S = Identifier.of("islandcore", "party_status_request_c2s");
	public static final Identifier PARTY_STATUS_S2C = Identifier.of("islandcore", "party_status_s2c");
	public static final Identifier PARTY_CREATE_C2S = Identifier.of("islandcore", "party_create_c2s");
	public static final Identifier PARTY_INVITE_C2S = Identifier.of("islandcore", "party_invite_c2s");
	public static final Identifier PARTY_ACCEPT_C2S = Identifier.of("islandcore", "party_accept_c2s");
	public static final Identifier PARTY_LEAVE_C2S = Identifier.of("islandcore", "party_leave_c2s");
	public static final Identifier PARTY_KICK_C2S = Identifier.of("islandcore", "party_kick_c2s");
	public static final Identifier PARTY_RENAME_C2S = Identifier.of("islandcore", "party_rename_c2s");
	public static final Identifier PARTY_DISBAND_REQUEST_C2S = Identifier.of("islandcore", "party_disband_request_c2s");
	public static final Identifier PARTY_DISBAND_CONFIRM_C2S = Identifier.of("islandcore", "party_disband_confirm_c2s");

	// Per-player ally-location-sharing preferences — see player.PlayerLocationSharingConfig.
	public static final Identifier LOCATION_SHARING_STATUS_REQUEST_C2S = Identifier.of("islandcore", "location_sharing_status_request_c2s");
	public static final Identifier LOCATION_SHARING_STATUS_S2C = Identifier.of("islandcore", "location_sharing_status_s2c");
	public static final Identifier LOCATION_SHARING_SET_C2S = Identifier.of("islandcore", "location_sharing_set_c2s");

	// Periodic push (not requested by the client) — see island.lifecycle.AllyLocationBroadcaster.
	public static final Identifier ALLY_LOCATIONS_S2C = Identifier.of("islandcore", "ally_locations_s2c");

	private NetworkChannels() {
	}
}
