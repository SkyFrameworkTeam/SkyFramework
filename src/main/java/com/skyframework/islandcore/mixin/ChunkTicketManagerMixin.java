package com.skyframework.islandcore.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import com.skyframework.islandcore.IslandCoreMod;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.util.math.ChunkSectionPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Defensive mitigation for a confirmed 100% vanilla NullPointerException, unrelated to our own
// code or to Fantasy — investigated in depth in an earlier session (crash: "Cannot invoke
// \"it.unimi.dsi.fastutil.objects.ObjectSet.remove(Object)\" because \"$$4\" is null", at
// ChunkTicketManager.handleChunkLeave, triggered while processing a player disconnect). Every
// frame of that crash's stack trace, mapped with the real Yarn 1.21.1+build.3 mappings, was inside
// net.minecraft.* — no islandcore, Fantasy, or Mixin frame anywhere in it.
//
// handleChunkLeave (decompiled with CFR to confirm, not guessed from bytecode) does:
//   ObjectSet<ServerPlayerEntity> set = (ObjectSet<ServerPlayerEntity>) this.playersByChunkPos.get(pos.toChunkPos().toLong());
//   set.remove(player);   // <- crashes here if the lookup returned null
//   if (set.isEmpty()) { ...cleans up distanceFromNearestPlayerTracker/nearbyChunkTicketUpdater/simulationDistanceTracker... }
// with no null check before the first line. We were unable to pin down what puts
// playersByChunkPos into this state for us specifically — but the exact same NPE (same message,
// same method) was independently reported in RelativityMC/C2ME-fabric#523, there confirmed to be
// caused by C2ME's own multithreaded chunk-ticket management (a mod we don't use). That confirms
// this exact spot is a known-fragile point in vanilla's single-threaded bookkeeping in general,
// even though our own trigger is a different, still-unconfirmed cause.
//
// Mitigation: if the lookup would return null, substitute a fresh, empty, MUTABLE ObjectSet
// instead of letting the null through. Deliberately NOT it.unimi.dsi.fastutil.objects.ObjectSets
// .emptySet() — decompiled that too: its remove(Object) unconditionally throws
// UnsupportedOperationException, which would just trade one crash for another. A real empty set
// makes the rest of the vanilla method run exactly as it would for a legitimately-just-emptied
// position: nothing is skipped, a missing entry is simply treated as equivalent to an empty one.
@Mixin(ChunkTicketManager.class)
public class ChunkTicketManagerMixin {

	@ModifyExpressionValue(
			method = "handleChunkLeave",
			at = @At(
					value = "INVOKE",
					target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"))
	private Object islandcore$fallBackToEmptySetOnNullChunkTracking(Object original, ChunkSectionPos pos, ServerPlayerEntity player) {
		if (original != null) {
			return original;
		}

		// Diagnostic only: lets us tell, from a normal server log, whether this still happens and
		// how often — without that, this mitigation would be silently invisible if it ever fires.
		IslandCoreMod.LOGGER.warn(
				"ChunkTicketManager.handleChunkLeave: no player set tracked for chunk section {} (player {}); "
						+ "substituting an empty one to avoid a known vanilla NullPointerException on disconnect. "
						+ "This indicates an upstream chunk-tracking desync we haven't root-caused — report if this appears often.",
				pos, player.getUuid());

		return new ObjectOpenHashSet<ServerPlayerEntity>();
	}
}
