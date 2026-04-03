package io.github.alvivar.stoneofmending;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

		Selection sel = SelectionManager.getOrCreate(player);
		if (!sel.hasA()) {
			player.sendOverlayMessage(Component.literal("Mark point A first (left-click)"));
			return InteractionResult.SUCCESS;
		}

		if (!context.getLevel().dimension().equals(sel.dimension())) {
			player.sendOverlayMessage(Component.literal("Point B must be in the same dimension as A"));
			return InteractionResult.SUCCESS;
		}

		BlockPos pos = context.getClickedPos().immutable();
		Direction normal = sel.normal();

		if (!isOnSamePlane(sel.pointA(), pos, normal)) {
			player.sendOverlayMessage(Component.literal("Point B must be on the same plane as A"));
			return InteractionResult.SUCCESS;
		}

		sel.markB(pos);
		SelectionManager.sync(player);
		player.sendOverlayMessage(Component.literal("Selection complete"));
		return InteractionResult.SUCCESS;
	}

	static boolean isOnSamePlane(BlockPos a, BlockPos b, Direction normal) {
		return switch (normal.getAxis()) {
			case X -> a.getX() == b.getX();
			case Y -> a.getY() == b.getY();
			case Z -> a.getZ() == b.getZ();
		};
	}
}
