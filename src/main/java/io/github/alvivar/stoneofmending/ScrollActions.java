package io.github.alvivar.stoneofmending;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
}
