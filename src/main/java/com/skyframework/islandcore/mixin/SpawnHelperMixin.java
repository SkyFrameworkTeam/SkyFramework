package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.NaturalMobSpawningProtectionListener;

import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SpawnHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// No Fabric API event exists for cancelling an individual natural mob spawn attempt in this
// version (same absence confirmed for explosions/crop trample). SpawnHelper#isValidSpawn is the
// last check before an already-created MobEntity is actually placed in the world, reached ONLY
// from the natural per-chunk spawn cycle (SpawnHelper#spawnEntitiesInChunk -> createMob ->
// isValidSpawn) — never from a mob spawner block or /summon, so this scopes cleanly to "natural"
// spawning as the flag name implies. Scoped further, here, to SpawnGroup.MONSTER (hostile) only —
// the investigation this implements was specifically about hostile mobs, and blocking passive
// animals/villagers too would be a much bigger behavior change than asked for.
@Mixin(SpawnHelper.class)
public class SpawnHelperMixin {

	@Inject(method = "isValidSpawn", at = @At("HEAD"), cancellable = true)
	private static void islandcore$blockNaturalHostileSpawn(
			ServerWorld world, MobEntity entity, double squaredDistanceToClosestPlayer, CallbackInfoReturnable<Boolean> cir) {
		if (entity.getType().getSpawnGroup() != SpawnGroup.MONSTER) {
			return;
		}
		if (NaturalMobSpawningProtectionListener.isSpawnBlocked(world, entity.getBlockPos())) {
			cir.setReturnValue(false);
		}
	}
}
