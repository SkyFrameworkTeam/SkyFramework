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

	private NetworkChannels() {
	}
}
