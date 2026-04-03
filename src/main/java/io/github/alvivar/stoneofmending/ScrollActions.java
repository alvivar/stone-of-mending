package io.github.alvivar.stoneofmending;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ScrollActions {

	public static void collect(ServerPlayer player, Selection sel) {
		ServerLevel level = (ServerLevel) player.level();
		SelectionBox box = SelectionBox.from(sel.pointA(), sel.pointB(), sel.normal());

		int collected = 0;
		for (BlockPos pos : box.slicePositions(sel.frontierOffset())) {
			if (!level.isLoaded(pos)) continue;

			BlockState state = level.getBlockState(pos);
			if (state.isAir()) continue;
			if (state.hasBlockEntity()) continue;

			Item item = state.getBlock().asItem();
			if (item == Items.AIR) continue;

			if (!level.removeBlock(pos, false)) continue;

			ItemStack stack = new ItemStack(item);
			if (!player.getInventory().add(stack)) {
				player.drop(stack, false);
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
		BlockState placeState = blockItem.getBlock().defaultBlockState();

		// Place one step outward from the current frontier
		int targetOffset = sel.frontierOffset() - 1;

		int placed = 0;
		for (BlockPos pos : box.slicePositions(targetOffset)) {
			if (offhand.isEmpty()) break;
			if (!level.isLoaded(pos)) continue;

			BlockState existing = level.getBlockState(pos);
			if (!existing.canBeReplaced()) continue;

			if (level.setBlock(pos, placeState, Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS)) {
				offhand.shrink(1);
				placed++;
			}
		}

		sel.advanceFrontier(-1);
		SelectionManager.sync(player);

		if (placed > 0) {
			player.sendOverlayMessage(Component.literal("Placed " + placed + " blocks"));
		} else {
			player.sendOverlayMessage(Component.literal("Nothing to place"));
		}
	}
}
