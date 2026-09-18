package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.PistonProtectionListener;

import net.minecraft.block.piston.PistonHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// No Fabric API event exists for intercepting piston pushes in this version (same situation as
// ExplosionMixin for explosions) — this is the only available hook, at the exact point vanilla
// itself decides whether the push succeeds (calculatePush's own return value), before
// PistonBlock#move ever applies movedBlocks/brokenBlocks to the world.
@Mixin(PistonHandler.class)
public class PistonHandlerMixin {

	@Shadow
	@Final
	private World world;

	@Shadow
	@Final
	private BlockPos posFrom;

	@Shadow
	@Final
	private Direction motionDirection;

	@Inject(method = "calculatePush", at = @At("RETURN"), cancellable = true)
	private void islandcore$blockOutOfBoundsPush(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()) {
			// Vanilla already refused the push on its own (blocked/too many blocks/etc.) — nothing
			// for island protection to add.
			return;
		}

		PistonHandler self = (PistonHandler) (Object) this;
		if (PistonProtectionListener.isPushBlocked(world, posFrom, motionDirection, self.getMovedBlocks())) {
			cir.setReturnValue(false);
		}
	}
}
