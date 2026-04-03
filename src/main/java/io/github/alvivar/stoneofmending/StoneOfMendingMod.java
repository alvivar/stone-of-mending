package io.github.alvivar.stoneofmending;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoneOfMendingMod implements ModInitializer {

	public static final String MOD_ID = "stone_of_mending";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.register();

		PayloadTypeRegistry.clientboundPlay().register(
				SelectionSyncPayload.TYPE, SelectionSyncPayload.STREAM_CODEC
		);

		PayloadTypeRegistry.serverboundPlay().register(
				ScrollActionC2SPayload.TYPE, ScrollActionC2SPayload.STREAM_CODEC
		);

		PayloadTypeRegistry.serverboundPlay().register(
				ClearSelectionC2SPayload.TYPE, ClearSelectionC2SPayload.STREAM_CODEC
		);

		ServerPlayNetworking.registerGlobalReceiver(ScrollActionC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!player.getMainHandItem().is(ModItems.STONE_OF_MENDING)) return;

			Selection sel = SelectionManager.getOrCreate(player);
			if (!sel.isComplete()) return;
			if (!player.level().dimension().equals(sel.dimension())) return;

			int dir = payload.direction();
			if (dir == -1) {
				ScrollActions.collect(player, sel);
			} else if (dir == 1) {
				ScrollActions.place(player, sel);
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(ClearSelectionC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!player.getMainHandItem().is(ModItems.STONE_OF_MENDING)) return;

			Selection sel = SelectionManager.get(player);
			if (sel == null || !sel.hasA()) return;

			sel.clear();
			SelectionManager.sync(player);
		});

		// Left-click marks point A
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			if (level.isClientSide()) {
				if (player.getMainHandItem().is(ModItems.STONE_OF_MENDING)) {
					return InteractionResult.SUCCESS;
				}
				return InteractionResult.PASS;
			}

			if (!player.getMainHandItem().is(ModItems.STONE_OF_MENDING)) {
				return InteractionResult.PASS;
			}

			ServerPlayer serverPlayer = (ServerPlayer) player;
			Selection sel = SelectionManager.getOrCreate(serverPlayer);
			sel.markA(pos.immutable(), direction, level.dimension());
			SelectionManager.sync(serverPlayer);
			serverPlayer.sendOverlayMessage(Component.literal("Point A marked"));
			return InteractionResult.SUCCESS;
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			SelectionManager.remove(handler.getPlayer());
		});

		LOGGER.info("Stone of Mending loaded");
	}
}
