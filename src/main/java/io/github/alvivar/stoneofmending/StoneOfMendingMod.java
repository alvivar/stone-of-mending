package io.github.alvivar.stoneofmending;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
