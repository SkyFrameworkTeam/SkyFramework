package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.RaidProtectionListener;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.raid.Raid;
import net.minecraft.village.raid.RaidManager;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// No Fabric API event exists for cancelling a raid start in this version (same absence confirmed
// for explosions/crop trample/natural spawning). RaidManager#startRaid is the exact trigger point
// (called from RaidOmenStatusEffect when a player with the Raid Omen effect stands in a village) —
// confirmed by bytecode that its only real-world caller discards the returned Raid entirely
// (RaidOmenStatusEffect: "startRaid(...); pop;"), so cancelling here and returning null is safe —
// nothing ever dereferences it.
@Mixin(RaidManager.class)
public class RaidManagerMixin {

	@Shadow
	@Final
	private ServerWorld world;

	@Inject(method = "startRaid", at = @At("HEAD"), cancellable = true)
	private void islandcore$blockRaidStart(ServerPlayerEntity player, BlockPos pos, CallbackInfoReturnable<Raid> cir) {
		if (RaidProtectionListener.isRaidBlocked(world, pos)) {
			cir.setReturnValue(null);
		}
	}
}
