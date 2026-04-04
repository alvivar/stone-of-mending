package io.github.alvivar.stoneofmending;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ScrollActions {

	private static final ItemStack VIRTUAL_PICKAXE = new ItemStack(Items.NETHERITE_PICKAXE);

	public static void collect(ServerPlayer player, Selection sel) {
		ServerLevel level = (ServerLevel) player.level();
		SelectionBox box = SelectionBox.from(sel.pointA(), sel.pointB(), sel.normal());

		// Abort if any position in the slice is unloaded
		for (BlockPos pos : box.slicePositions(sel.frontierOffset())) {
			if (!level.isLoaded(pos)) {
				player.sendOverlayMessage(Component.literal("Slice not fully loaded"));
				return;
			}
		}

		int collected = 0;
		for (BlockPos pos : box.slicePositions(sel.frontierOffset())) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir())
				continue;
			if (state.hasBlockEntity())
				continue;

			List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, VIRTUAL_PICKAXE);

			if (!level.removeBlock(pos, false))
				continue;

			for (ItemStack drop : drops) {
				if (!player.getInventory().add(drop)) {
					player.drop(drop, false);
				}
			}
			collected++;
		}

		sel.advanceFrontier(1);
		SelectionManager.sync(player);

		if (collected > 0) {
			player.sendOverlayMessage(Component.literal("Collected " + collected + " blocks"));
		} else {
			player.sendOverlayMessage(Component.literal("Nothing to collect"));
		}
	}

	public static void place(ServerPlayer player, Selection sel) {
		ServerLevel level = (ServerLevel) player.level();
		ItemStack offhand = player.getOffhandItem();

		if (offhand.isEmpty() || !(offhand.getItem() instanceof BlockItem blockItem)) {
			player.sendOverlayMessage(Component.literal("Hold a block in your offhand"));
			return;
		}

		SelectionBox box = SelectionBox.from(sel.pointA(), sel.pointB(), sel.normal());

		// Scan outward from frontier for the first incomplete slice
		Integer targetOffset = findNextPlacement(level, box, sel.frontierOffset());
		if (targetOffset == null) {
			player.sendOverlayMessage(Component.literal("Nothing to place"));
			return;
		}

		BlockState placeState = blockItem.getBlock().defaultBlockState();
		ItemStack template = offhand.copy();

		int placed = 0;
		for (BlockPos pos : box.slicePositions(targetOffset)) {
			if (offhand.isEmpty()) {
				offhand = refillOffhand(player, template);
				if (offhand.isEmpty())
					break;
			}
			if (!level.isLoaded(pos))
				continue;
			if (level.isOutsideBuildHeight(pos))
				continue;

			BlockState existing = level.getBlockState(pos);
			if (!existing.canBeReplaced())
				continue;

			if (level.setBlock(pos, placeState, Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS)) {
				offhand.shrink(1);
				placed++;
			}
		}

		if (placed > 0) {
			sel.setFrontier(targetOffset);
			SelectionManager.sync(player);
			player.sendOverlayMessage(Component.literal("Placed " + placed + " blocks"));
		} else {
			player.sendOverlayMessage(Component.literal("Nothing to place"));
		}
	}

	public static void replace(ServerPlayer player, Selection sel) {
		ServerLevel level = (ServerLevel) player.level();
		ItemStack offhand = player.getOffhandItem();

		if (offhand.isEmpty() || !(offhand.getItem() instanceof BlockItem blockItem)) {
			player.sendOverlayMessage(Component.literal("Hold a block in your offhand"));
			return;
		}

		SelectionBox box = SelectionBox.from(sel.pointA(), sel.pointB(), sel.normal());
		BlockState placeState = blockItem.getBlock().defaultBlockState();
		Block targetBlock = blockItem.getBlock();
		ItemStack template = offhand.copy();
		int depth = box.depth();

		int replaced = 0;
		for (int offset = depth - 1; offset >= 0; offset--) {
			for (BlockPos pos : box.slicePositions(offset)) {
				if (offhand.isEmpty()) {
					offhand = refillOffhand(player, template);
					if (offhand.isEmpty())
						break;
				}
				if (!level.isLoaded(pos))
					continue;

				BlockState state = level.getBlockState(pos);
				if (state.isAir())
					continue;
				if (state.getBlock() instanceof LiquidBlock)
					continue;
				if (state.hasBlockEntity())
					continue;
				if (state.is(targetBlock))
					continue;

				List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, VIRTUAL_PICKAXE);

				if (level.setBlock(pos, placeState, Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS)) {
					offhand.shrink(1);
					for (ItemStack drop : drops) {
						if (!player.getInventory().add(drop)) {
							player.drop(drop, false);
						}
					}
					replaced++;
				}
			}

			// Check refill between slices too
			if (offhand.isEmpty()) {
				offhand = refillOffhand(player, template);
				if (offhand.isEmpty())
					break;
			}
		}

		if (replaced > 0) {
			player.sendOverlayMessage(Component.literal("Replaced " + replaced + " blocks"));
		} else {
			player.sendOverlayMessage(Component.literal("Nothing to replace"));
		}
	}

	// --- Placement scan ---

	private enum SliceStatus {
		COMPLETE, INCOMPLETE, BLOCKED
	}

	/**
	 * Scans outward from the frontier for the first incomplete slice.
	 * Starts at frontierOffset-1 and goes outward (decreasing offset).
	 * Skips already-complete slices. Returns null if blocked or nothing found.
	 */
	private static Integer findNextPlacement(ServerLevel level, SelectionBox box, int frontierOffset) {
		for (int offset = frontierOffset - 1;; offset--) {
			switch (checkSlice(level, box, offset)) {
				case INCOMPLETE -> {
					return offset;
				}
				case BLOCKED -> {
					return null;
				}
				case COMPLETE -> {
				} // continue past full slices
			}
		}
	}

	private static SliceStatus checkSlice(ServerLevel level, SelectionBox box, int offset) {
		boolean anyReplaceable = false;
		boolean anyInBounds = false;

		for (BlockPos pos : box.slicePositions(offset)) {
			if (level.isOutsideBuildHeight(pos))
				continue;
			anyInBounds = true;
			if (!level.isLoaded(pos))
				return SliceStatus.BLOCKED;
			if (level.getBlockState(pos).canBeReplaced())
				anyReplaceable = true;
		}

		// Entire slice out of world bounds — can't go further
		if (!anyInBounds)
			return SliceStatus.BLOCKED;
		return anyReplaceable ? SliceStatus.INCOMPLETE : SliceStatus.COMPLETE;
	}

	// --- Inventory helpers ---

	private static ItemStack refillOffhand(ServerPlayer player, ItemStack template) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < 36; i++) {
			ItemStack slot = inventory.getItem(i);
			if (ItemStack.isSameItemSameComponents(slot, template)) {
				inventory.setItem(Inventory.SLOT_OFFHAND, slot);
				inventory.setItem(i, ItemStack.EMPTY);
				return player.getOffhandItem();
			}
		}
		return ItemStack.EMPTY;
	}
}
