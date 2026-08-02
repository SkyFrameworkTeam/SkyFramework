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

	// How resolveSeed() actually arrived at `seed`, persisted verbatim rather than re-derived from
	// whether `seed` is present — a resolved RANDOM seed and a CUSTOM one are both just "a present
	// long" once resolved, so the seed value alone can't tell them apart after the fact. KEEP is the
	// only case `seed` stays null (VanillaResetExecutor keeps the dimension's existing seed).
	public enum SeedMode {
		RANDOM,
		KEEP,
		CUSTOM
	}

	private final String dimensionKey;
	private final Long seed;
	private final SeedMode seedMode;
	private final UUID requestedBy;
	private final Status status;
	// When this entry was confirmed: used by VanillaResetExecutor to pick which entry's seed wins
	// when several queued entries each specify one (only one level.dat seed can be applied).
	private final Instant timestamp;

	public PendingVanillaReset(String dimensionKey, Long seed, SeedMode seedMode, UUID requestedBy, Status status, Instant timestamp) {
		this.dimensionKey = dimensionKey;
		this.seed = seed;
		this.seedMode = seedMode;
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

	public SeedMode getSeedMode() {
		return seedMode;
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
		json.addProperty("seedMode", seedMode.name());
		json.addProperty("requestedBy", requestedBy.toString());
		json.addProperty("status", status.name());
		json.addProperty("timestamp", timestamp.toEpochMilli());
		return json;
	}

	public static PendingVanillaReset fromJson(JsonObject json) {
		String dimensionKey = json.get("dimensionKey").getAsString();
		Long seed = json.has("seed") ? json.get("seed").getAsLong() : null;
		// Absent on entries written before this field existed: best-effort reconstruction from the
		// seed value alone (the same ambiguity this field exists to remove going forward) — a
		// present seed reads back as CUSTOM, absent as KEEP; RANDOM can't be recovered for old
		// entries since it was never distinguishable from CUSTOM before this field existed.
		SeedMode seedMode = json.has("seedMode")
				? SeedMode.valueOf(json.get("seedMode").getAsString())
				: (seed != null ? SeedMode.CUSTOM : SeedMode.KEEP);
		UUID requestedBy = UUID.fromString(json.get("requestedBy").getAsString());
		Status status = Status.valueOf(json.get("status").getAsString());
		// Absent on entries written before this field existed: treat as "oldest possible" so a
		// genuinely-timestamped entry always wins the seed-selection tiebreak over it.
		Instant timestamp = json.has("timestamp") ? Instant.ofEpochMilli(json.get("timestamp").getAsLong()) : Instant.EPOCH;
		return new PendingVanillaReset(dimensionKey, seed, seedMode, requestedBy, status, timestamp);
	}
}
