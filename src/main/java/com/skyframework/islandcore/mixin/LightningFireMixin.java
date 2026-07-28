package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.FireProtectionListener;

import net.minecraft.entity.LightningEntity;
import net.minecraft.util.math.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningEntity.class)
public class LightningFireMixin {

	@Shadow
	private BlockPos getAffectedBlockPos() {
		throw new AssertionError();
	}

	@Inject(method = "spawnFire", at = @At("HEAD"), cancellable = true)
	private void islandcore$blockLightningFire(int count, CallbackInfo ci) {
		LightningEntity self = (LightningEntity) (Object) this;
		if (!FireProtectionListener.isFireSpreadAllowed(self.getWorld(), getAffectedBlockPos())) {
			ci.cancel();
		}
	}
}
