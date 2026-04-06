package io.github.alvivar.stoneofmending;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoneOfMendingMod implements ModInitializer {

	public static final String MOD_ID = "stone_of_mending";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

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

		PayloadTypeRegistry.serverboundPlay().register(
				MiddleClickC2SPayload.TYPE, MiddleClickC2SPayload.STREAM_CODEC
		);

		PayloadTypeRegistry.serverboundPlay().register(
				SetNormalC2SPayload.TYPE, SetNormalC2SPayload.STREAM_CODEC
		);

		ServerPlayNetworking.registerGlobalReceiver(ScrollActionC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!player.getMainHandItem().is(ModItems.STONE_OF_MENDING)) return;

			Selection sel = SelectionManager.get(player);
			if (sel == null || !sel.isComplete()) return;
			if (!player.level().dimension().equals(sel.dimension())) return;

			int dir = payload.direction();
			boolean shifted = payload.shifted();
			if (shifted) {
				if (dir == 1) ScrollActions.interiorFill(player, sel);
				else if (dir == -1) ScrollActions.interiorCollect(player, sel);
			} else {
				if (dir == -1) ScrollActions.collect(player, sel);
				else if (dir == 1) ScrollActions.place(player, sel);
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(MiddleClickC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!player.getMainHandItem().is(ModItems.STONE_OF_MENDING)) return;

			Selection sel = SelectionManager.get(player);
			if (sel == null || !sel.isComplete()) return;
			if (!player.level().dimension().equals(sel.dimension())) return;

			ScrollActions.replace(player, sel);
		});

		ServerPlayNetworking.registerGlobalReceiver(SetNormalC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!player.getMainHandItem().is(ModItems.STONE_OF_MENDING)) return;

			Selection sel = SelectionManager.get(player);
			if (sel == null || !sel.hasA()) return;
			if (!player.level().dimension().equals(sel.dimension())) return;

			Direction newNormal = normalFromLook(player.getLookAngle());
			sel.setNormal(newNormal);
			sel.setFrontier(0);
			SelectionManager.sync(player);
			player.sendOverlayMessage(Component.literal("The stone now faces " + directionName(newNormal) + "."));
		});

		ServerPlayNetworking.registerGlobalReceiver(ClearSelectionC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!player.getMainHandItem().is(ModItems.STONE_OF_MENDING)) return;

			Selection sel = SelectionManager.get(player);
			if (sel == null || !sel.hasA()) return;

			SelectionManager.remove(player);
			ServerPlayNetworking.send(player, SelectionSyncPayload.from(new Selection()));
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
			serverPlayer.sendOverlayMessage(Component.literal("The stone remembers the first mark."));
			return InteractionResult.SUCCESS;
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			SelectionManager.remove(handler.getPlayer());
		});

		ServerTickEvents.END_SERVER_TICK.register(SelectionManager::tick);

		LOGGER.info("Stone of Mending loaded");
	}

	private static Direction normalFromLook(Vec3 look) {
		double ax = Math.abs(look.x);
		double ay = Math.abs(look.y);
		double az = Math.abs(look.z);
		if (ax >= ay && ax >= az) return look.x >= 0 ? Direction.EAST : Direction.WEST;
		if (ay >= az) return look.y >= 0 ? Direction.UP : Direction.DOWN;
		return look.z >= 0 ? Direction.SOUTH : Direction.NORTH;
	}

	private static String directionName(Direction dir) {
		return switch (dir) {
			case UP -> "Up";
			case DOWN -> "Down";
			case NORTH -> "North";
			case SOUTH -> "South";
			case EAST -> "East";
			case WEST -> "West";
		};
	}
}
