package com.skyframework.islandcore.dimension.vanilla;

import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.UUID;

// PENDING_CONFIRMATION only ever lives in memory inside VanillaResetService (mirrors
// IslandDeletionServiceImpl's own in-memory-only pending state); IN_PROGRESS is the only status
// ever written to pending_vanilla_reset.json, since that's the only one VanillaResetExecutor needs
// to survive a server restart and resume/retry on the next boot.
//
// pending_vanilla_reset.json holds a JSON ARRAY of these (one per queued dimension, at most one per
// dimension key at a time) so several resets (e.g. Overworld + Nether) can be queued up before a
// single restart. toJson()/fromJson() define that per-entry schema in one place so
// VanillaResetService (writer) and VanillaResetExecutor (reader) can't drift apart on field names.
public class PendingVanillaReset {

	public enum Status {
		PENDING_CONFIRMATION,
		IN_PROGRESS
	}

	private final String dimensionKey;
	private final Long seed;
	private final UUID requestedBy;
	private final Status status;
	// When this entry was confirmed: used by VanillaResetExecutor to pick which entry's seed wins
	// when several queued entries each specify one (only one level.dat seed can be applied).
	private final Instant timestamp;

	public PendingVanillaReset(String dimensionKey, Long seed, UUID requestedBy, Status status, Instant timestamp) {
		this.dimensionKey = dimensionKey;
		this.seed = seed;
		this.requestedBy = requestedBy;
		this.status = status;
		this.timestamp = timestamp;
	}

	public String getDimensionKey() {
		return dimensionKey;
	}

	public Long getSeed() {
		return seed;
	}

	public UUID getRequestedBy() {
		return requestedBy;
	}

	public Status getStatus() {
		return status;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("dimensionKey", dimensionKey);
		if (seed != null) {
			json.addProperty("seed", seed);
		}
		json.addProperty("requestedBy", requestedBy.toString());
		json.addProperty("status", status.name());
		json.addProperty("timestamp", timestamp.toEpochMilli());
		return json;
	}

	public static PendingVanillaReset fromJson(JsonObject json) {
		String dimensionKey = json.get("dimensionKey").getAsString();
		Long seed = json.has("seed") ? json.get("seed").getAsLong() : null;
		UUID requestedBy = UUID.fromString(json.get("requestedBy").getAsString());
		Status status = Status.valueOf(json.get("status").getAsString());
		// Absent on entries written before this field existed: treat as "oldest possible" so a
		// genuinely-timestamped entry always wins the seed-selection tiebreak over it.
		Instant timestamp = json.has("timestamp") ? Instant.ofEpochMilli(json.get("timestamp").getAsLong()) : Instant.EPOCH;
		return new PendingVanillaReset(dimensionKey, seed, requestedBy, status, timestamp);
	}
}
