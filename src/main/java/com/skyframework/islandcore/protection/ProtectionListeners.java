package com.skyframework.islandcore.protection;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;

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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ProtectionListeners {

	private static final Text NO_PERMISSION_MESSAGE = Text.literal("No tienes permiso para hacer esto aquí.");
	private static final Text NO_ISLAND_MESSAGE = Text.literal("Esta zona no pertenece a ninguna isla.");
	private static final Text RESERVED_PLOT_MESSAGE =
			Text.literal("Esta zona está reservada para una futura ampliación de la isla.");

	private ProtectionListeners() {
	}

	// Only meaningful to call after an action was denied: outside the islands dimension,
	// AccessController always allows, so a deny here always means we're inside it. Mirrors
	// AccessControllerImpl's classification purely to pick the right message.
	private static Text denyMessage(BlockPos pos) {
		Island island = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(pos).orElse(null);
		if (island == null) {
			return NO_ISLAND_MESSAGE;
		}
		if (!island.getBounds().contains(pos)) {
			return RESERVED_PLOT_MESSAGE;
		}
		return NO_PERMISSION_MESSAGE;
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

		player.sendMessage(denyMessage(pos), false);
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

		player.sendMessage(denyMessage(checkedPos), false);
		return ActionResult.FAIL;
	}

	private static ActionResult onUseEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return ActionResult.PASS;
		}

		if (IslandCoreMod.ACCESS_CONTROLLER.canInteractEntity(player.getUuid(), serverWorld, entity)) {
			return ActionResult.PASS;
		}

		player.sendMessage(denyMessage(entity.getBlockPos()), false);
		return ActionResult.FAIL;
	}

	private static ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return ActionResult.PASS;
		}

		if (IslandCoreMod.ACCESS_CONTROLLER.canAttackEntity(player.getUuid(), serverWorld, entity)) {
			return ActionResult.PASS;
		}

		player.sendMessage(denyMessage(entity.getBlockPos()), false);
		return ActionResult.FAIL;
	}
}
