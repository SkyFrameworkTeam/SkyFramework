package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.FireProtectionListener;

import net.minecraft.block.FireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public class FireSpreadMixin {

	// trySpreadingFire is called once per candidate neighbor per tick; pos is the block about
	// to be ignited. Cancelling only this call blocks that one ignition, leaving the rest of
	// fire's tick behavior (aging, burning out, etc.) untouched.
	@Inject(method = "trySpreadingFire", at = @At("HEAD"), cancellable = true)
	private void islandcore$blockFireSpread(
			World world, BlockPos pos, int spreadFactor, Random random, int currentAge, CallbackInfo ci) {
		if (!FireProtectionListener.isFireSpreadAllowed(world, pos)) {
			ci.cancel();
		}
	}
}
