package com.skyframework.islandcore.teleport;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

// Internal bookkeeping for a teleport currently counting down its warmup; not part of the public api/.
class PendingTeleport {

	// Which cooldown map/completion message applies once this finishes (see TeleportManagerImpl).
	enum Kind {
		HOME,
		SPAWN,
		FARMING,
		// Any DIMENSION_REGISTRY dimension reached through the dynamic teleports section, other
		// than the one matching FarmingConfig's own target (that one still uses Kind.FARMING — see
		// TeleportManagerImpl#requestDimensionTeleport).
		DIMENSION,
		// Neither the target dimension's own landing point nor a nearby safe spot could be found
		// (see TeleportManagerImpl#resolveDynamicDimensionLanding) — the player was bounced to their
		// own island's home or the Spawn island's home instead, as a safety net, same "no cooldown,
		// this wasn't a deliberate action" reasoning as VoidRescueListener. Kept distinct from
		// Kind.HOME/Kind.SPAWN so completeTeleport doesn't charge either cooldown for an involuntary
		// rescue.
		RESCUE_HOME,
		RESCUE_SPAWN,
		// Fixed teleport to the real vanilla minecraft:overworld dimension (TeleportsScreen's
		// "Overworld" button) — kept distinct from DIMENSION since it's never a DIMENSION_REGISTRY
		// entry, so completeTeleport's DIMENSION case display-name lookup would never find it.
		OVERWORLD
	}

	final UUID playerUuid;
	final RegistryKey<World> targetDimension;
	final BlockPos targetPos;
	final Vec3d startPosition;
	final Kind kind;
	int ticksRemaining;

	PendingTeleport(UUID playerUuid, RegistryKey<World> targetDimension, BlockPos targetPos, Vec3d startPosition, int ticksRemaining, Kind kind) {
		this.playerUuid = playerUuid;
		this.targetDimension = targetDimension;
		this.targetPos = targetPos;
		this.startPosition = startPosition;
		this.ticksRemaining = ticksRemaining;
		this.kind = kind;
	}
}
