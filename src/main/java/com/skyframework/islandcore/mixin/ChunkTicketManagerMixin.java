package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.IslandCoreMod;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.util.math.ChunkSectionPos;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Defensive mitigation for a confirmed 100% vanilla NullPointerException, unrelated to our own
// code — full root-cause investigation done separately (decompiled with CFR against the real
// Yarn 1.21.1+build.3 mappings): handleChunkLeave assumes playersByChunkPos.get(l) always returns
// a non-null ObjectSet, because handleChunkEnter is the ONLY place that ever populates it (lazily,
// via computeIfAbsent) — handleChunkLeave itself never checks for null before calling .remove(...)
// on whatever get(l) returned. That symmetry can break: confirmed independently in pure vanilla via
// RelativityMC/C2ME-fabric#523 (there caused by C2ME's own multithreaded chunk-ticket access, a mod
// we don't use), and suspected here to be related to Fantasy's non-standard persistent-dimension
// teardown/recreation cycle (Fantasy's own RuntimeWorldManager#delete/#unload skip vanilla's usual
// multi-step, drain-before-close sequence; see NucleoidMC/fantasy#78 for the maintainer's own
// acknowledgment of a related conflict between Fantasy's player-kicking and normal per-world player
// bookkeeping). The exact trigger for OUR crashes remains unconfirmed — every captured stack trace
// was pure net.minecraft.*, no islandcore/Fantasy/Mixin frame in it — so this mitigation targets the
// symptom, not Fantasy's lifecycle itself (out of scope here; the library's own maintainer hasn't
// resolved it across several open issues).
//
// Chosen fix: bail out of handleChunkLeave ENTIRELY when the lookup is missing, touching nothing
// else — distanceFromNearestPlayerTracker/nearbyChunkTicketUpdater/simulationDistanceTracker are
// left completely alone, since if playersByChunkPos never had an entry for this position there's no
// reason to believe those trackers have one either. (Rejected alternative: substitute a fresh empty
// ObjectSet and let the rest of the method run against it — that would still exercise the "position
// is now empty" cleanup branch against trackers that may never have been populated for this
// position in the first place, which isn't obviously correct and isn't needed just to avoid the
// crash.) Scope is deliberately narrow: only this one null-check, no other method on
// ChunkTicketManager and no change to Fantasy's own lifecycle.
@Mixin(ChunkTicketManager.class)
public class ChunkTicketManagerMixin {

	@Shadow
	@Final
	Long2ObjectMap<ObjectSet<ServerPlayerEntity>> playersByChunkPos;

	// Plain @Inject rather than @Redirect/@ModifyExpressionValue on the playersByChunkPos.get(l)
	// call itself: a @Redirect (or MixinExtras' @ModifyExpressionValue) can only replace the VALUE
	// of that one expression — neither has any way to make the enclosing handleChunkLeave method
	// return early, which is exactly what this fix needs (skip the rest of the method's logic
	// entirely, not feed it a substitute value). @Inject at HEAD with cancellable=true is the
	// standard Mixin idiom for "skip this method under condition X" and needs no MixinExtras — it
	// re-derives the exact same lookup key (pos.toChunkPos().toLong()) the real method body computes
	// a few lines later, purely to decide whether to bail out before any of vanilla's own logic runs.
	@Inject(method = "handleChunkLeave", at = @At("HEAD"), cancellable = true)
	private void islandcore$skipIfNoPlayerSetTracked(ChunkSectionPos pos, ServerPlayerEntity player, CallbackInfo ci) {
		long chunkPosLong = pos.toChunkPos().toLong();
		if (this.playersByChunkPos.get(chunkPosLong) == null) {
			// INFO, not WARN/ERROR: this is a known, deliberately-tolerated vanilla/Fantasy edge
			// case (see class javadoc above), not a real failure — kept visible at INFO (rather than
			// DEBUG) specifically so it shows up in a normal server log without needing debug
			// logging enabled, so we can tell how often it still fires after this mitigation.
			IslandCoreMod.LOGGER.info(
					"ChunkTicketManager.handleChunkLeave: no player set tracked for chunk section {} (player {}); "
							+ "skipping — this is a known, tolerated vanilla/Fantasy edge case, not a real failure. "
							+ "Report if this appears very frequently.",
					pos, player.getUuid());
			ci.cancel();
		}
	}
}
