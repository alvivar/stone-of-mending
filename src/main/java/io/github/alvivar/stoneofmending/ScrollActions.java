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
		int targetOffset = sel.frontierOffset() - 1;

		if (checkSlice(level, box, targetOffset) == SliceStatus.BLOCKED) {
			player.sendOverlayMessage(Component.literal("Slice is blocked"));
			return;
		}

		sel.setFrontier(targetOffset);
		SelectionManager.sync(player);

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

	// --- Interior operations (Shift+scroll) ---

	public static void interiorFill(ServerPlayer player, Selection sel) {
		ServerLevel level = (ServerLevel) player.level();
		ItemStack offhand = player.getOffhandItem();

		if (offhand.isEmpty() || !(offhand.getItem() instanceof BlockItem blockItem)) {
			player.sendOverlayMessage(Component.literal("Hold a block in your offhand"));
			return;
		}

		SelectionBox box = SelectionBox.from(sel.pointA(), sel.pointB(), sel.normal());

		Integer targetOffset = findNextInteriorFill(level, box, sel.frontierOffset());
		if (targetOffset == null) {
			player.sendOverlayMessage(Component.literal("Nothing to fill"));
			return;
		}

		BlockState placeState = blockItem.getBlock().defaultBlockState();
		ItemStack template = offhand.copy();

		int placed = 0;
		for (BlockPos pos : box.slicePositions(targetOffset)) {
			if (offhand.isEmpty()) {
				offhand = refillOffhand(player, template);
				if (offhand.isEmpty()) break;
			}
			if (!level.isLoaded(pos)) continue;
			if (level.isOutsideBuildHeight(pos)) continue;

			BlockState existing = level.getBlockState(pos);
			if (!existing.canBeReplaced()) continue;

			if (level.setBlock(pos, placeState, Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS)) {
				offhand.shrink(1);
				placed++;
			}
		}

		if (placed > 0) {
			int current = sel.frontierOffset();
			int depth = box.depth();
			// Move cursor unless in front of box (shift+up can't retract from front)
			if (current >= 0) {
				int next = (current <= depth - 1)
						? Math.max(0, Math.min(targetOffset, depth - 1))
						: targetOffset;
				sel.setFrontier(next);
				SelectionManager.sync(player);
			}
			player.sendOverlayMessage(Component.literal("Filled " + placed + " blocks"));
		} else {
			player.sendOverlayMessage(Component.literal("Nothing to fill"));
		}
	}

	public static void interiorCollect(ServerPlayer player, Selection sel) {
		ServerLevel level = (ServerLevel) player.level();
		SelectionBox box = SelectionBox.from(sel.pointA(), sel.pointB(), sel.normal());

		Integer targetOffset = findNextInteriorCollect(level, box, sel.frontierOffset());
		if (targetOffset == null) {
			player.sendOverlayMessage(Component.literal("Nothing to collect"));
			return;
		}

		int collected = 0;
		for (BlockPos pos : box.slicePositions(targetOffset)) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir()) continue;
			if (state.hasBlockEntity()) continue;

			List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, VIRTUAL_PICKAXE);

			if (!level.removeBlock(pos, false)) continue;

			for (ItemStack drop : drops) {
				if (!player.getInventory().add(drop)) {
					player.drop(drop, false);
				}
			}
			collected++;
		}

		if (collected > 0) {
			int current = sel.frontierOffset();
			int depth = box.depth();
			// Move cursor unless past far side (shift+down can't retract from far)
			if (current <= depth - 1) {
				int next = (current >= 0)
						? Math.max(0, Math.min(targetOffset + 1, depth - 1))
						: targetOffset + 1;
				sel.setFrontier(next);
				SelectionManager.sync(player);
			}
			player.sendOverlayMessage(Component.literal("Collected " + collected + " blocks"));
		} else {
			player.sendOverlayMessage(Component.literal("Nothing to collect"));
		}
	}

	// --- Placement scan ---

	private enum SliceStatus {
		COMPLETE, INCOMPLETE, BLOCKED
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

	// --- Interior scan helpers ---

	/** Finds the deepest incomplete slice in the operating range. Scans far→face. Aborts on blocked. */
	private static Integer findNextInteriorFill(ServerLevel level, SelectionBox box, int frontierOffset) {
		int upper = Math.max(frontierOffset - 1, box.depth() - 1);
		int lower = Math.min(frontierOffset, 0);
		for (int offset = upper; offset >= lower; offset--) {
			switch (checkSlice(level, box, offset)) {
				case INCOMPLETE -> { return offset; }
				case BLOCKED -> { return null; }
				case COMPLETE -> {}
			}
		}
		return null;
	}

	/** Finds the first slice with collectible blocks in the operating range. Scans face→far. Aborts on blocked. */
	private static Integer findNextInteriorCollect(ServerLevel level, SelectionBox box, int frontierOffset) {
		int lower = Math.min(frontierOffset, 0);
		int upper = Math.max(frontierOffset - 1, box.depth() - 1);
		for (int offset = lower; offset <= upper; offset++) {
			int result = sliceCollectStatus(level, box, offset);
			if (result == 1) return offset;
			if (result == -1) return null;
		}
		return null;
	}

	/** Returns 1 = has collectible blocks, 0 = empty/nothing to collect, -1 = blocked (unloaded). */
	private static int sliceCollectStatus(ServerLevel level, SelectionBox box, int offset) {
		boolean found = false;
		for (BlockPos pos : box.slicePositions(offset)) {
			if (level.isOutsideBuildHeight(pos)) continue;
			if (!level.isLoaded(pos)) return -1;
			BlockState state = level.getBlockState(pos);
			if (!state.isAir() && !state.hasBlockEntity()) found = true;
		}
		return found ? 1 : 0;
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
