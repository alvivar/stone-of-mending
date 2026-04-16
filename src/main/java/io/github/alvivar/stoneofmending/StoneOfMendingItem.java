package io.github.alvivar.stoneofmending;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class StoneOfMendingItem extends Item {

	public StoneOfMendingItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getHand() != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		if (context.getLevel().isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (!(context.getPlayer() instanceof ServerPlayer player)) {
			return InteractionResult.PASS;
		}

		Selection sel = SelectionManager.get(player);
		if (sel == null || !sel.hasA()) {
			ScrollActions.playSound(player, SoundEvents.AMETHYST_BLOCK_HIT, 0.3f);
			player.sendOverlayMessage(Component.literal("The stone needs a first mark."));
			return InteractionResult.SUCCESS;
		}

		if (!context.getLevel().dimension().equals(sel.dimension())) {
			ScrollActions.playSound(player, SoundEvents.AMETHYST_BLOCK_HIT, 0.3f);
			player.sendOverlayMessage(Component.literal("The stone cannot bind two worlds."));
			return InteractionResult.SUCCESS;
		}

		BlockPos clicked = context.getClickedPos().immutable();

		// If stroke is in progress, commit: derive a fresh A from the current frontier face
		// opposite the clicked B, so reshape tracks where the player actually is.
		if (sel.isComplete() && sel.frontierOffset() != 0) {
			SelectionBox box = SelectionBox.from(sel.pointA(), sel.pointB(), sel.normal());
			int faceCoord = box.frontierBlock(sel.frontierOffset());

			int newAx = box.minX(), newAy = box.minY(), newAz = box.minZ();
			switch (sel.normal().getAxis()) {
				case X -> {
					newAx = faceCoord;
					newAy = fartherBound(clicked.getY(), box.minY(), box.maxY());
					newAz = fartherBound(clicked.getZ(), box.minZ(), box.maxZ());
				}
				case Y -> {
					newAx = fartherBound(clicked.getX(), box.minX(), box.maxX());
					newAy = faceCoord;
					newAz = fartherBound(clicked.getZ(), box.minZ(), box.maxZ());
				}
				case Z -> {
					newAx = fartherBound(clicked.getX(), box.minX(), box.maxX());
					newAy = fartherBound(clicked.getY(), box.minY(), box.maxY());
					newAz = faceCoord;
				}
			}
			sel.setPoints(new BlockPos(newAx, newAy, newAz), clicked);
			sel.setFrontier(0);
		} else {
			sel.markB(clicked);
		}

		SelectionManager.sync(player);
		ScrollActions.playSound(player, SoundEvents.ENCHANTMENT_TABLE_USE, 0.5f);
		player.sendOverlayMessage(Component.literal("The shape is whole."));
		return InteractionResult.SUCCESS;
	}

	private static int fartherBound(int clicked, int min, int max) {
		return Math.abs(clicked - min) >= Math.abs(clicked - max) ? min : max;
	}
}
