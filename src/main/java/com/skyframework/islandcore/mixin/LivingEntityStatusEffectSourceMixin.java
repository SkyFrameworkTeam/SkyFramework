package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.StatusEffectSourceTracker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Investigated with real decompiled evidence (CFR, Yarn 1.21.1+build.3): LivingEntity#addStatusEffect
// (the (StatusEffectInstance, Entity) overload) is the ONLY point in vanilla where a harmful status
// effect's applying entity is still known — PoisonStatusEffect#applyUpdateEffect (the periodic tick
// that actually deals the damage) uses DamageSources.magic(), which carries no source/attacker at
// all. See StatusEffectSourceTracker's class doc for the full picture.
@Mixin(LivingEntity.class)
public abstract class LivingEntityStatusEffectSourceMixin {

	@Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",
			at = @At("HEAD"))
	private void islandcore$recordHostileEffectSource(
			StatusEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
		if (!(source instanceof PlayerEntity) || effect.getEffectType().value().isBeneficial()) {
			return;
		}

		LivingEntity self = (LivingEntity) (Object) this;
		StatusEffectSourceTracker.record(self.getUuid(), source.getUuid(), effect.getDuration());
	}
}
