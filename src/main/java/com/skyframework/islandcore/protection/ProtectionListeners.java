package com.skyframework.islandcore.protection;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.util.ServerLang;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ProtectionListeners {

	private ProtectionListeners() {
	}

	// Only meaningful to call after an action was denied: outside the islands dimension,
	// AccessController always allows, so a deny here always means we're inside it. Mirrors
	// AccessControllerImpl's classification purely to pick the right message. Was 3 static final
	// Text constants built once at class-load time — turned into a per-call method so each one can
	// be picked per the RECEIVING player's own client language (see ServerLang) instead of a single
	// server-wide value baked in at startup.
	private static Text denyMessage(BlockPos pos, ServerPlayerEntity player) {
		Island island = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(pos).orElse(null);
		if (island == null) {
			return ServerLang.of(player, "Esta zona no pertenece a ninguna isla.", "This area doesn't belong to any island.");
		}
		if (!island.getBounds().contains(pos) && !isBeyondWorldHeightLimit(pos)) {
			return ServerLang.of(player, "Esta zona está reservada para una futura ampliación de la isla.",
					"This area is reserved for a future island expansion.");
		}
		return ServerLang.of(player, "No tienes permiso para hacer esto aquí.", "You don't have permission to do this here.");
	}

	// Y=320 (one above the highest buildable layer) and Y=-64 (the world's own bottom — see
	// IslandRegistryImpl's MIN_Y/MAX_Y, which already sets every island's own vertical bounds to
	// exactly this same range) are the world's own absolute vertical limits, not this island's
	// plot edge. Vanilla already shows its own "outside the world" message there, so
	// RESERVED_PLOT_MESSAGE — meant for the island's horizontal/plot boundary — would just be a
	// confusing, redundant second message stacked on top of it.
	private static boolean isBeyondWorldHeightLimit(BlockPos pos) {
		return pos.getY() >= 320 || pos.getY() <= -64;
	}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register(ProtectionListeners::onBlockBreak);
		UseBlockCallback.EVENT.register(ProtectionListeners::onUseBlock);
		UseEntityCallback.EVENT.register(ProtectionListeners::onUseEntity);
		AttackEntityCallback.EVENT.register(ProtectionListeners::onAttackEntity);
	}

	private static boolean onBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return true;
		}

		if (IslandCoreMod.ACCESS_CONTROLLER.canBreak(player.getUuid(), serverWorld, pos)) {
			return true;
		}

		DeniedActionThrottler.notifyDenied(player, denyMessage(pos, (ServerPlayerEntity) player));
		return false;
	}

	private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return ActionResult.PASS;
		}

		BlockPos clickedPos = hitResult.getBlockPos();
		ItemStack heldStack = player.getStackInHand(hand);

		BlockPos checkedPos;
		boolean allowed;
		if (heldStack.getItem() instanceof BlockItem) {
			// Heuristic: holding a placeable block is treated as an attempt to place at the clicked face,
			// rather than trying to replicate vanilla's exact interact-vs-place priority resolution.
			checkedPos = clickedPos.offset(hitResult.getSide());
			allowed = IslandCoreMod.ACCESS_CONTROLLER.canPlace(player.getUuid(), serverWorld, checkedPos);
		} else {
			checkedPos = clickedPos;
			BlockState state = serverWorld.getBlockState(clickedPos);
			NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(serverWorld, clickedPos);
			allowed = screenHandlerFactory != null
					? IslandCoreMod.ACCESS_CONTROLLER.canOpenContainer(player.getUuid(), serverWorld, clickedPos)
					: IslandCoreMod.ACCESS_CONTROLLER.canInteractBlock(player.getUuid(), serverWorld, clickedPos);
		}

		if (allowed) {
			return ActionResult.PASS;
		}

		DeniedActionThrottler.notifyDenied(player, denyMessage(checkedPos, (ServerPlayerEntity) player));
		return ActionResult.FAIL;
	}

	private static ActionResult onUseEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return ActionResult.PASS;
		}

		if (IslandCoreMod.ACCESS_CONTROLLER.canInteractEntity(player.getUuid(), serverWorld, entity)) {
			return ActionResult.PASS;
		}

		DeniedActionThrottler.notifyDenied(player, denyMessage(entity.getBlockPos(), (ServerPlayerEntity) player));
		return ActionResult.FAIL;
	}

	private static ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return ActionResult.PASS;
		}

		if (IslandCoreMod.ACCESS_CONTROLLER.canAttackEntity(player.getUuid(), serverWorld, entity)) {
			return ActionResult.PASS;
		}

		DeniedActionThrottler.notifyDenied(player, denyMessage(entity.getBlockPos(), (ServerPlayerEntity) player));
		return ActionResult.FAIL;
	}
}
