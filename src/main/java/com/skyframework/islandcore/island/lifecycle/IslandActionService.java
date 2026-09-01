package com.skyframework.islandcore.island.lifecycle;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.network.ActionOutcome;
import com.skyframework.islandcore.api.network.ActionReason;
import com.skyframework.islandcore.api.permission.IslandPermissions;
import com.skyframework.islandcore.island.biome.BiomeTier;
import com.skyframework.islandcore.island.generation.BasicPlatformGenerator;
import com.skyframework.islandcore.island.model.IslandSetting;
import com.skyframework.islandcore.protection.flag.Flag;
import com.skyframework.islandcore.protection.flag.FlagCategory;
import com.skyframework.islandcore.protection.flag.FlagRegistry;
import com.skyframework.islandcore.protection.flag.TriState;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Extracted from IslandCommand (Sprint "acciones de isla"): every method here reuses the same
 * IslandRegistryApi/PermissionProvider/BiomeTierRegistry/IslandBiomeApplier/IslandDeletionService
 * calls the text command already made, just without formatting a chat message — callers (the
 * text command, and now network packet handlers) turn the returned {@link ActionOutcome} into
 * whatever presentation they need. No method here sends a message to the player itself.
 */
public final class IslandActionService {

	public static final RegistryKey<World> ISLANDS_DIMENSION =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("islandcore", "islands"));

	private static final BasicPlatformGenerator PLATFORM_GENERATOR = new BasicPlatformGenerator();

	private IslandActionService() {
	}

	public static ActionOutcome<Island> create(ServerPlayerEntity player, MinecraftServer server) {
		UUID playerUuid = player.getUuid();

		if (IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, IslandPermissions.ISLAND_CREATE_DENY)) {
			return ActionOutcome.fail(ActionReason.NO_PERMISSION);
		}

		Island island;
		try {
			island = IslandCoreMod.ISLAND_REGISTRY.createIsland(playerUuid, ISLANDS_DIMENSION);
		} catch (IllegalStateException e) {
			return ActionOutcome.fail(ActionReason.ALREADY_HAS_ISLAND);
		}

		BlockPos center = island.getCenter();
		ServerWorld islandsWorld = server.getWorld(ISLANDS_DIMENSION);
		PLATFORM_GENERATOR.generate(islandsWorld, center, island.getIslandSize());

		return ActionOutcome.ok(island);
	}

	public record UpgradeResult(int oldSize, int newSize) {
	}

	// oldSize == newSize means "already at the max permitted size" — a success, not a failure
	// (matches /island upgrade's existing behavior: it replies with feedback, not an error, and
	// still returns 1).
	public static ActionOutcome<UpgradeResult> upgrade(UUID playerUuid) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(playerUuid);
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		int maxSize = IslandCoreMod.PERMISSION_PROVIDER.getHighestSizeAllowed(playerUuid);
		int currentSize = island.getIslandSize();

		if (maxSize <= currentSize) {
			return ActionOutcome.ok(new UpgradeResult(currentSize, currentSize));
		}

		try {
			IslandCoreMod.ISLAND_REGISTRY.resizeIsland(island.getIslandId(), maxSize);
		} catch (IllegalArgumentException e) {
			return ActionOutcome.fail(ActionReason.PLOT_SIZE_EXCEEDED);
		}

		return ActionOutcome.ok(new UpgradeResult(currentSize, maxSize));
	}

	public static ActionOutcome<Void> updateSetting(UUID playerUuid, IslandSetting setting, boolean value) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(playerUuid);
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		// getIslandByOwner already only returns islands this player literally owns, so this is
		// currently redundant, but kept explicit per the original command's own comment, in case
		// that lookup ever changes (e.g. members getting their own island-lookup path).
		if (!island.getOwnerUuid().equals(playerUuid)) {
			return ActionOutcome.fail(ActionReason.NOT_OWNER);
		}

		IslandCoreMod.ISLAND_REGISTRY.updateIslandSetting(island.getIslandId(), setting, value);
		return ActionOutcome.ok();
	}

	// The SINGLE entry point both /island settings (IslandCommand#executeSettings) and the
	// IslandSettingsUpdateC2S network handler call for a legacy setting id (firespread/pvp/
	// mobdamage/buildprotection) — neither caller duplicates the id-to-flag mapping or decides
	// between updateFlag/updateSetting itself, so the two paths can never drift onto different
	// storage again. firespread/pvp/mobdamage are rerouted to the new Flag override system
	// (fire_spread/pvp_damage/mob_damage, ISLAND_GLOBAL); buildprotection is deliberately left on
	// the old IslandSetting-keyed path — it was never part of the "motor de flags" migration.
	public static ActionOutcome<Void> updateLegacySetting(UUID playerUuid, String settingId, boolean value) {
		String flagId = flagIdForLegacySetting(settingId);
		if (flagId != null) {
			Flag flag = FlagRegistry.get(flagId).orElseThrow();
			return updateFlag(playerUuid, flag, TriState.fromBoolean(value));
		}

		Optional<IslandSetting> maybeSetting = IslandSetting.fromId(settingId);
		if (maybeSetting.isEmpty()) {
			return ActionOutcome.fail(ActionReason.UNKNOWN_SETTING);
		}
		return updateSetting(playerUuid, maybeSetting.get(), value);
	}

	private static String flagIdForLegacySetting(String settingId) {
		return switch (settingId.toLowerCase(Locale.ROOT)) {
			case "firespread" -> "fire_spread";
			case "pvp" -> "pvp_damage";
			case "mobdamage" -> "mob_damage";
			default -> null;
		};
	}

	// Shared by both /island flags set (any flag) and updateLegacySetting above — dispatches to the
	// matching override map by the flag's own category.
	public static ActionOutcome<Void> updateFlag(UUID playerUuid, Flag flag, TriState value) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(playerUuid);
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		if (!island.getOwnerUuid().equals(playerUuid)) {
			return ActionOutcome.fail(ActionReason.NOT_OWNER);
		}

		if (flag.getCategory() == FlagCategory.ROLE_BASED) {
			IslandCoreMod.ISLAND_REGISTRY.updateRoleFlagOverride(island.getIslandId(), flag.getId(), value);
		} else {
			IslandCoreMod.ISLAND_REGISTRY.updateGlobalFlagOverride(island.getIslandId(), flag.getId(), value);
		}
		return ActionOutcome.ok();
	}

	// Shared by both "/island flags preset" and FlagSetPresetC2S. flagId/preset are passed through
	// as raw strings, not pre-resolved — IslandRegistryApi#applyFlagPreset itself validates flagId
	// resolves to a ROLE_BASED flag and preset is one of FlagPreset's 4 names, throwing
	// IllegalArgumentException otherwise, caught here and reported as one reason (INVALID_FLAG_PRESET)
	// regardless of which of the two validations actually failed — same "one catch-all reason"
	// simplification INVALID_STYLE/INVALID_DIMENSION_ID already use for their own single-cause
	// validation failures.
	public static ActionOutcome<Void> applyFlagPreset(UUID playerUuid, String flagId, String preset) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(playerUuid);
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		if (!island.getOwnerUuid().equals(playerUuid)) {
			return ActionOutcome.fail(ActionReason.NOT_OWNER);
		}

		try {
			IslandCoreMod.ISLAND_REGISTRY.applyFlagPreset(island.getIslandId(), flagId, preset);
		} catch (IllegalArgumentException e) {
			return ActionOutcome.fail(ActionReason.INVALID_FLAG_PRESET);
		}
		return ActionOutcome.ok();
	}

	// Shared by both "/island exceptions preset" and ExceptionGroupSetPresetC2S. groupId/preset are
	// passed through as raw strings, not pre-resolved — IslandRegistryApi#applyExceptionGroupPreset
	// itself validates groupId resolves to a registered ExceptionGroup and preset is one of
	// FlagPreset's 4 names, throwing IllegalArgumentException otherwise, caught here and reported as
	// one reason (INVALID_EXCEPTION_PRESET) — exact mirror of applyFlagPreset above.
	public static ActionOutcome<Void> applyExceptionGroupPreset(UUID playerUuid, String groupId, String preset) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(playerUuid);
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		if (!island.getOwnerUuid().equals(playerUuid)) {
			return ActionOutcome.fail(ActionReason.NOT_OWNER);
		}

		try {
			IslandCoreMod.ISLAND_REGISTRY.applyExceptionGroupPreset(island.getIslandId(), groupId, preset);
		} catch (IllegalArgumentException e) {
			return ActionOutcome.fail(ActionReason.INVALID_EXCEPTION_PRESET);
		}
		return ActionOutcome.ok();
	}

	public static ActionOutcome<Void> requestDelete(UUID requestedBy) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(requestedBy);
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		try {
			IslandCoreMod.DELETION_SERVICE.requestDeletion(maybeIsland.get().getIslandId(), requestedBy);
		} catch (IllegalStateException e) {
			return ActionOutcome.fail(ActionReason.ISLAND_ALREADY_DELETING);
		}

		return ActionOutcome.ok();
	}

	public static ActionOutcome<Void> confirmDelete(UUID requestedBy) {
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(requestedBy);
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		boolean confirmed = IslandCoreMod.DELETION_SERVICE.confirmDeletion(maybeIsland.get().getIslandId(), requestedBy);
		if (!confirmed) {
			return ActionOutcome.fail(ActionReason.NO_PENDING_DELETION);
		}

		return ActionOutcome.ok();
	}

	/**
	 * data holds different types depending on the failure reason (documented per case below),
	 * matching the original command's need for extra detail beyond a flat message — {@code null}
	 * on success and on any reason not listed:
	 * <ul>
	 *   <li>{@link ActionReason#BIOME_LOCKED}: {@code List<String>} tier ids containing the
	 *   biome (possibly empty, meaning the biome isn't in any configured tier at all)</li>
	 *   <li>{@link ActionReason#COOLDOWN_ACTIVE}: {@code Long} seconds remaining</li>
	 * </ul>
	 */
	public static ActionOutcome<Object> changeBiome(ServerPlayerEntity player, Identifier biomeId, MinecraftServer server) {
		UUID playerUuid = player.getUuid();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(playerUuid);
		if (maybeIsland.isEmpty()) {
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		if (!island.getOwnerUuid().equals(playerUuid)) {
			return ActionOutcome.fail(ActionReason.NOT_OWNER);
		}

		Registry<Biome> biomeRegistry = server.getRegistryManager().get(RegistryKeys.BIOME);
		Optional<RegistryEntry.Reference<Biome>> maybeBiome = biomeRegistry.getEntry(biomeId);
		if (maybeBiome.isEmpty()) {
			return ActionOutcome.fail(ActionReason.BIOME_NOT_FOUND);
		}

		if (!IslandCoreMod.BIOME_TIER_REGISTRY.canUse(playerUuid, biomeId, IslandCoreMod.PERMISSION_PROVIDER)) {
			List<BiomeTier> tiers = IslandCoreMod.BIOME_TIER_REGISTRY.getTiersContaining(biomeId);
			List<String> tierIds = tiers.stream().map(BiomeTier::id).toList();
			return new ActionOutcome<>(false, ActionReason.BIOME_LOCKED, tierIds);
		}

		if (!IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, IslandPermissions.BIOME_COOLDOWN_BYPASS)) {
			Instant lastChange = island.getLastBiomeChangeAt();
			if (lastChange != null) {
				long cooldownSeconds = IslandCoreMod.PERMISSION_PROVIDER.getBiomeCooldownSeconds(playerUuid);
				Instant availableAt = lastChange.plusSeconds(cooldownSeconds);
				if (Instant.now().isBefore(availableAt)) {
					long remainingSeconds = java.time.Duration.between(Instant.now(), availableAt).getSeconds();
					return new ActionOutcome<>(false, ActionReason.COOLDOWN_ACTIVE, remainingSeconds);
				}
			}
		}

		ServerWorld world = server.getWorld(island.getDimension());
		if (world == null) {
			return ActionOutcome.fail(ActionReason.DIMENSION_UNAVAILABLE);
		}

		IslandCoreMod.BIOME_APPLIER.enqueue(world, island.getPlotBounds(), maybeBiome.get(), playerUuid);
		// Recorded now, at enqueue time, not once IslandBiomeApplier's multi-tick chunk-by-chunk
		// job actually finishes touching every block — same convention as
		// updateLastBiomeChangeAt right below (also written immediately, before the world catches
		// up) and as IslandDeletionServiceImpl marking DELETING immediately on confirm. The
		// applier has no crash-resume mechanism either way, so deferring to completion wouldn't
		// remove the inconsistency window, just move it — and the player is already told this
		// "puede tardar unos segundos" by the caller's own chat message.
		IslandCoreMod.ISLAND_REGISTRY.updateCurrentBiomeId(island.getIslandId(), biomeId.toString());
		IslandCoreMod.ISLAND_REGISTRY.updateLastBiomeChangeAt(island.getIslandId(), Instant.now());

		return ActionOutcome.ok();
	}
}
