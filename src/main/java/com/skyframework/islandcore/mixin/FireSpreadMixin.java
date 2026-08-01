package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.FireProtectionListener;

import net.minecraft.block.FireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

	// getBurnChance(WorldView, BlockPos) is private and, confirmed by decompiling FireBlock, has
	// exactly one call site in the whole class: scheduledTick's own separate 3x3x6 ambient-ignition
	// loop, which sets nearby AIR blocks on fire directly via world.setBlockState(...), entirely
	// without ever calling trySpreadingFire above. Redirecting this exact overload therefore can't
	// reach any other call site — there isn't one — and can't be confused with the other
	// getBurnChance(BlockState) overload (used by isFlammable() and elsewhere), since Mixin matches
	// by full descriptor, not by name.
	@Shadow
	private int getBurnChance(WorldView world, BlockPos pos) {
		throw new AssertionError();
	}

	// Forces that loop's own "if (p <= 0) continue;" check to skip the position entirely — exactly
	// as if nothing flammable were nearby — without touching trySpreadingFire's already-correct
	// behavior, and with zero effect when firespread=true (falls through to the real burn chance).
	@Redirect(
			method = "scheduledTick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/block/FireBlock;getBurnChance(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;)I"))
	private int islandcore$blockAmbientFireSpread(FireBlock instance, WorldView world, BlockPos pos) {
		if (world instanceof World actualWorld && !FireProtectionListener.isFireSpreadAllowed(actualWorld, pos)) {
			return 0;
		}
		return this.getBurnChance(world, pos);
	}
}
