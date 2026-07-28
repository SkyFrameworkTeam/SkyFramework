package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.ExplosionProtectionListener;

import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// No Fabric API event exists for intercepting explosion block lists in this version
// (confirmed absent from fabric-api 0.116.14+1.21.1 and its upstream source tree),
// so this mixin is the only available hook.
@Mixin(Explosion.class)
public class ExplosionMixin {

	@Shadow
	@Final
	private World world;

	@Inject(method = "affectWorld", at = @At("HEAD"))
	private void islandcore$blockTerrainDamage(boolean particleEffects, CallbackInfo ci) {
		if (ExplosionProtectionListener.isTerrainDamageBlocked(world)) {
			((Explosion) (Object) this).clearAffectedBlocks();
		}
	}
}
