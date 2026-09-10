package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.CropTrampleProtectionListener;

import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// No Fabric API event exists for intercepting farmland trample in this version (same absence
// confirmed for explosions — see ExplosionMixin). FarmlandBlock#onLandedUpon does two independent
// things in sequence: maybe convert this block to dirt (the trample), then unconditionally call
// super.onLandedUpon(...) to apply fall damage — an @Inject cancelling the whole method would also
// suppress fall damage, which crop_trample has nothing to do with. @Redirect on just the
// setToDirt(...) call instead leaves fall damage untouched and only ever skips the dirt
// conversion.
@Mixin(FarmlandBlock.class)
public class FarmlandTrampleMixin {

	@Redirect(method = "onLandedUpon", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/block/FarmlandBlock;setToDirt(Lnet/minecraft/entity/Entity;Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"))
	private static void islandcore$maybeSetToDirt(Entity entity, BlockState state, World world, BlockPos pos) {
		if (CropTrampleProtectionListener.isTrampleBlocked(world, pos)) {
			return;
		}
		FarmlandBlock.setToDirt(entity, state, world, pos);
	}
}
