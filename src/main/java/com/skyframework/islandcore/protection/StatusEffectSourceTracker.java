package com.skyframework.islandcore.protection;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Fills the gap DamageSources.magic() leaves open: a harmful status effect's periodic tick damage
// (e.g. PoisonStatusEffect#applyUpdateEffect) uses that shared constant, which carries NEITHER a
// source NOR an attacker — by the time the effect ticks, vanilla has already discarded who applied
// it. LivingEntityStatusEffectSourceMixin records that attacker here, at the one point it's still
// known (LivingEntity#addStatusEffect), so DamageProtectionListener can still attribute the later
// tick damage to a player for the PVP_DAMAGE check, the same as an instant potion effect already can
// via DamageSources.indirectMagic (see the investigation this fix is based on).
//
// Keyed by VICTIM only, not by effect type: only one hostile-effect source is remembered per victim
// at a time (the most recent), which is enough for the reported bug (a thrown potion's poison) and
// avoids the extra bookkeeping multiple concurrent effect instances would need for a case that, in
// practice, doesn't come up — a player is rarely poisoned by two different players at once.
public final class StatusEffectSourceTracker {

	private record Entry(UUID attackerUuid, Instant expiresAt) {
	}

	private static final Map<UUID, Entry> sourcesByVictim = new ConcurrentHashMap<>();

	private StatusEffectSourceTracker() {
	}

	// durationTicks is the effect's OWN duration (20 ticks/second) — the record expires shortly
	// after the effect itself would have, so a later, unrelated (e.g. self-inflicted) instance of
	// the same or another hostile effect never gets attributed to a stale attacker.
	public static void record(UUID victimUuid, UUID attackerUuid, int durationTicks) {
		long safeDurationTicks = Math.max(durationTicks, 1);
		sourcesByVictim.put(victimUuid, new Entry(attackerUuid, Instant.now().plusMillis(safeDurationTicks * 50L)));
	}

	public static Optional<UUID> getHostileEffectSource(UUID victimUuid) {
		Entry entry = sourcesByVictim.get(victimUuid);
		if (entry == null) {
			return Optional.empty();
		}
		if (Instant.now().isAfter(entry.expiresAt())) {
			sourcesByVictim.remove(victimUuid);
			return Optional.empty();
		}
		return Optional.of(entry.attackerUuid());
	}
}
