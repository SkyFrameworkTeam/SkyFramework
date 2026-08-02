package com.skyframework.islandcore.net;

import net.minecraft.util.Identifier;

// Single source of truth for every "islandcore" custom payload channel. Handshake identifiers
// here MUST match IslandCoreClient's network.handshake.ClientHandshakeC2S/ServerHandshakeS2C
// exactly (that mod hardcodes them directly, with no shared module between the two projects) —
// this is the reason both sides live under the "islandcore" namespace rather than
// "islandcoreclient": the server owns this protocol.
public final class NetworkChannels {

	public static final int PROTOCOL_VERSION = 1;

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
	public static final Identifier MEMBER_TRUST_C2S = Identifier.of("islandcore", "member_trust_c2s");
	public static final Identifier MEMBER_REMOVE_C2S = Identifier.of("islandcore", "member_remove_c2s");

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

	private NetworkChannels() {
	}
}
