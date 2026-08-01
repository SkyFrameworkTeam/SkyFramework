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

	private NetworkChannels() {
	}
}
