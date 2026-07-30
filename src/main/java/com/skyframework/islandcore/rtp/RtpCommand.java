package com.skyframework.islandcore.rtp;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.permission.IslandPermissions;
import com.skyframework.islandcore.teleport.TeleportBackend;
import com.skyframework.islandcore.teleport.VanillaTeleportBackend;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// "/rtp", open to every player (no OP required).
//
// The per-player cooldown below is kept in memory only, never persisted to disk: a server
// restart resets everyone's /rtp cooldown. That's acceptable here — unlike /island home there's
// no per-destination state to protect, just rate-limiting a "send me somewhere safe" button.
public class RtpCommand {

	private static final SafeRandomTeleportFinder FINDER = new SafeRandomTeleportFinder();
	private static final TeleportBackend TELEPORT_BACKEND = new VanillaTeleportBackend();

	private static final Map<UUID, Instant> lastRtpAt = new HashMap<>();

	private RtpCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("rtp")
						.executes(RtpCommand::executeRtp))
		);
	}

	private static int executeRtp(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		UUID playerUuid = player.getUuid();
		ServerWorld world = player.getServerWorld();

		if (!IslandCoreMod.RTP_CONFIG.isEnabled()) {
			source.sendError(Text.literal("El comando /rtp está desactivado en este servidor."));
			return 0;
		}

		if (!IslandCoreMod.RTP_CONFIG.isAllowed(world.getRegistryKey().getValue())) {
			source.sendError(Text.literal("No puedes usar /rtp en esta dimensión."));
			return 0;
		}

		if (!IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, IslandPermissions.RTP_COOLDOWN_BYPASS)) {
			Instant last = lastRtpAt.get(playerUuid);
			if (last != null) {
				long cooldownSeconds = IslandCoreMod.PERMISSION_PROVIDER.getRtpCooldownSeconds(playerUuid);
				Instant availableAt = last.plusSeconds(cooldownSeconds);
				if (Instant.now().isBefore(availableAt)) {
					String remaining = formatDuration(Duration.between(Instant.now(), availableAt));
					source.sendError(Text.literal("Todavía no puedes volver a usar /rtp. Podrás hacerlo en " + remaining + "."));
					return 0;
				}
			}
		}

		Optional<BlockPos> maybePos = FINDER.findSafeLocation(world, IslandCoreMod.RTP_CONFIG);
		if (maybePos.isEmpty()) {
			// No cooldown applied: don't penalize the player for bad luck finding a spot.
			source.sendError(Text.literal("No se ha podido encontrar un lugar seguro. Inténtalo de nuevo."));
			return 0;
		}

		TELEPORT_BACKEND.teleport(player, world, maybePos.get());
		lastRtpAt.put(playerUuid, Instant.now());

		source.sendFeedback(() -> Text.literal("¡Teletransportado a una ubicación aleatoria!"), false);

		return 1;
	}

	private static String formatDuration(Duration duration) {
		long totalSeconds = Math.max(1, duration.getSeconds());
		long days = totalSeconds / 86400;
		long hours = (totalSeconds % 86400) / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;

		if (days > 0) {
			return days + (days == 1 ? " día " : " días ") + hours + (hours == 1 ? " hora" : " horas");
		}
		if (hours > 0) {
			return hours + (hours == 1 ? " hora " : " horas ") + minutes + (minutes == 1 ? " minuto" : " minutos");
		}
		if (minutes > 0) {
			return minutes + (minutes == 1 ? " minuto " : " minutos ") + seconds + (seconds == 1 ? " segundo" : " segundos");
		}
		return seconds + (seconds == 1 ? " segundo" : " segundos");
	}
}
