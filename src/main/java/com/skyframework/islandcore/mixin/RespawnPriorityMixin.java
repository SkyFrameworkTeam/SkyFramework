package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

// Investigated with real decompiled evidence (CFR, Yarn 1.21.1+build.3) before touching any code:
// vanilla's own ServerPlayerEntity#getRespawnTarget has NO restriction tying bed/anchor respawn to
// "the same dimension the player died in" — it looks up this.server.getWorld(this.getSpawnPointDimension()),
// i.e. whatever dimension the bed/anchor itself is in, unconditionally. The only real gates are
// BedBlock.isBedWorking(world) (= world.getDimension().bedWorks(), the BED's own dimension) and
// RespawnAnchorBlock.isNether(world) (the anchor's own dimension) — both already correctly scoped
// to the bed/anchor's dimension, not the player's. So a valid bed/anchor in ANY dimension already
// works as-is today; nothing here needs to (or does) touch that path.
//
// What vanilla does NOT do is fall back to anything but the Overworld default spawn when there is
// no valid bed/anchor (TeleportTarget.missingSpawnBlock(this.server.getOverworld(), ...)). That
// fallback chain — player's own island home, then the Spawn island's home — is exactly the gap this
// Mixin fills, and only that gap: it never overrides a target vanilla already resolved from a real
// bed/anchor (missingRespawnBlock() false is left completely untouched).
//
// @Inject at RETURN with a CallbackInfoReturnable, rather than redirecting the bed/anchor lookup
// itself: this needs to inspect the ALREADY-COMPUTED vanilla result (specifically its
// missingRespawnBlock() flag) and only conditionally replace it — exactly what
// CallbackInfoReturnable#getReturnValue()/#setReturnValue() gives for free at a RETURN injection
// point, with no MixinExtras required.
@Mixin(ServerPlayerEntity.class)
public abstract class RespawnPriorityMixin {

	@Inject(method = "getRespawnTarget", at = @At("RETURN"), cancellable = true)
	private void islandcore$applyIslandRespawnFallback(
			boolean alive, TeleportTarget.PostDimensionTransition postDimensionTransition, CallbackInfoReturnable<TeleportTarget> cir) {
		TeleportTarget vanillaTarget = cir.getReturnValue();
		if (!vanillaTarget.missingRespawnBlock()) {
			// Valid bed/anchor found by vanilla (in whatever dimension it's actually in) — respect it
			// unchanged, no matter which dimension the player died in or which dimension the bed is in.
			return;
		}

		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;

		Optional<Island> ownIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(self.getUuid());
		TeleportTarget fallback = islandHomeTarget(self, ownIsland.orElse(null), postDimensionTransition);
		if (fallback == null) {
			Optional<Island> spawnIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
			fallback = islandHomeTarget(self, spawnIsland.orElse(null), postDimensionTransition);
		}

		if (fallback != null) {
			cir.setReturnValue(fallback);
		}
		// Neither the player's own island nor the Spawn island has a usable home: leave vanilla's
		// own Overworld-default fallback (already in cir's return value) untouched.
	}

	// Null if island is null, has no home location set, or its dimension isn't currently loaded —
	// in every one of those cases the caller is expected to try the next fallback (or, if this was
	// already the last one, leave vanilla's own default in place).
	private static TeleportTarget islandHomeTarget(
			ServerPlayerEntity self, Island island, TeleportTarget.PostDimensionTransition postDimensionTransition) {
		if (island == null) {
			return null;
		}

		BlockPos home = island.getHomeLocation();
		if (home == null) {
			return null;
		}

		ServerWorld homeWorld = self.server.getWorld(island.getDimension());
		if (homeWorld == null) {
			return null;
		}

		Vec3d pos = new Vec3d(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
		return new TeleportTarget(homeWorld, pos, Vec3d.ZERO, self.getYaw(), 0.0f, postDimensionTransition);
	}
}
