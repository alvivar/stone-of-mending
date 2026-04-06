package io.github.alvivar.stoneofmending;

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
			player.sendOverlayMessage(Component.literal("The stone needs a first mark."));
			return InteractionResult.SUCCESS;
		}

		if (!context.getLevel().dimension().equals(sel.dimension())) {
			player.sendOverlayMessage(Component.literal("The stone cannot bind two worlds."));
			return InteractionResult.SUCCESS;
		}

		sel.markB(context.getClickedPos().immutable());
		SelectionManager.sync(player);
		player.sendOverlayMessage(Component.literal("The shape is whole."));
		return InteractionResult.SUCCESS;
	}
}
