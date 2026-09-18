package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.IslandCoreMod;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.TeleportTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Investigated with real decompiled evidence (CFR, Yarn 1.21.1+build.3): ServerPlayerEntity#teleportTo
// is the single choke point every cross-dimension player teleport funnels through — portals,
// respawn, and this mod's own VanillaTeleportBackend (via the older ServerPlayerEntity#teleport
// overload, which itself calls teleportTo for a cross-dimension target). At HEAD, this.getPos() and
// this.getServerWorld() still reflect the OLD dimension: by the time teleportTo's own worldChanged()
// call runs (further down, after setServerWorld/requestTeleport), the position has already been
// overwritten with the new one, which is why this Mixin injects at HEAD instead of there.
@Mixin(ServerPlayerEntity.class)
public abstract class PlayerLastPositionMixin {

	@Inject(method = "teleportTo", at = @At("HEAD"))
	private void islandcore$recordLastPositionBeforeDimensionChange(TeleportTarget teleportTarget, CallbackInfoReturnable<Entity> cir) {
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		if (self.isRemoved()) {
			return;
		}

		ServerWorld currentWorld = self.getServerWorld();
		if (teleportTarget.world().getRegistryKey() == currentWorld.getRegistryKey()) {
			// Same-dimension teleport: nothing to record, this player never "left" a dimension.
			return;
		}

		IslandCoreMod.PLAYER_LAST_POSITION_STORE.recordLastPosition(
				self.getUuid(), currentWorld.getRegistryKey().getValue(), self.getBlockPos());
	}
}
