package com.skyframework.islandcore.teleport;

import com.skyframework.islandcore.api.network.ActionOutcome;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.UUID;

public interface TeleportManager {

	// requestHome/requestSpawn/requestFarming keep messaging the player directly (as before this
	// sprint's ActionOutcome return type was added) rather than leaving it to the caller: they
	// need to message the player again later, asynchronously, from tickAll()'s warmup countdown —
	// a context with no ServerCommandSource/network Context to reply through, only the stored
	// player reference. The returned ActionOutcome is purely so a future network handler can
	// build its immediate ActionResultS2C without re-deriving the failure reason itself; text
	// command callers keep ignoring it exactly as they ignored the previous void return.
	ActionOutcome<Void> requestHome(ServerPlayerEntity player);

	ActionOutcome<Void> requestSpawn(ServerPlayerEntity player);

	ActionOutcome<Void> requestFarming(ServerPlayerEntity player);

	// Dynamic-teleports section (Sprint "teletransportes dinámicos"): dimensionId is any
	// DIMENSION_REGISTRY dimension. When it equals FarmingConfig's own target, this delegates
	// straight to requestFarming (same cooldown, same safe-landing-at-world-spawn behavior, same
	// PendingTeleport.Kind.FARMING) instead of duplicating that logic — every other dimension has
	// no cooldown of its own and lands at the player's last known position in it (or the world's
	// spawn point, safe-landing-corrected, on a first visit).
	ActionOutcome<Void> requestDimensionTeleport(ServerPlayerEntity player, Identifier dimensionId);

	// Unlike the three above, /rtp is a single synchronous action (no warmup countdown), so this
	// one follows the same "pure outcome, caller formats every message" convention as
	// IslandActionService/MembershipService instead. data carries the remaining cooldown in
	// seconds (Long) only when reason == ActionReason.COOLDOWN_ACTIVE, null otherwise.
	ActionOutcome<Long> requestRtp(ServerPlayerEntity player);

	// Called once per server tick by IslandCoreMod's ServerTickEvents.END_SERVER_TICK listener;
	// not meant to be called by other systems.
	void tickAll();

	void cancelPendingTeleport(UUID playerUuid, String reason);

	// Read-only peeks added for TeleportStatusS2C (Sprint "acciones de isla"): 0 if no cooldown is
	// currently active (including when the player holds the relevant bypass permission), matching
	// exactly the cooldown math already in requestHome/requestSpawn/requestFarming/requestRtp
	// without mutating any state or messaging the player.
	long getHomeCooldownRemainingSeconds(UUID playerUuid);

	long getSpawnCooldownRemainingSeconds(UUID playerUuid);

	long getFarmingCooldownRemainingSeconds(UUID playerUuid);

	long getRtpCooldownRemainingSeconds(UUID playerUuid);

	// Same peek convention as the four above: 0 unless dimensionId equals FarmingConfig's own
	// target, in which case it mirrors getFarmingCooldownRemainingSeconds — every other dimension
	// has no cooldown of its own.
	long getDimensionCooldownRemainingSeconds(UUID playerUuid, Identifier dimensionId);
}
