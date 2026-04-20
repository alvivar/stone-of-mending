package io.github.alvivar.stoneofmending;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StoneOfMendingMod implements ModInitializer {

	public static final String MOD_ID = "stone_of_mending";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.register();

		PayloadTypeRegistry.clientboundPlay().register(
				SelectionSyncPayload.TYPE, SelectionSyncPayload.STREAM_CODEC);

		PayloadTypeRegistry.serverboundPlay().register(
				ScrollActionC2SPayload.TYPE, ScrollActionC2SPayload.STREAM_CODEC);

		PayloadTypeRegistry.serverboundPlay().register(
				ClearSelectionC2SPayload.TYPE, ClearSelectionC2SPayload.STREAM_CODEC);

		PayloadTypeRegistry.serverboundPlay().register(
				MiddleClickC2SPayload.TYPE, MiddleClickC2SPayload.STREAM_CODEC);

		PayloadTypeRegistry.serverboundPlay().register(
				SetNormalC2SPayload.TYPE, SetNormalC2SPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ScrollActionC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!player.getMainHandItem().is(ModItems.STONE_OF_MENDING))
				return;

			Selection sel = SelectionManager.get(player);
			if (sel == null || !sel.isComplete())
				return;
			if (!player.level().dimension().equals(sel.dimension()))
				return;

			int dir = payload.direction();
			if (payload.shifted()) {
				if (dir == 1)
					ScrollActions.interiorFill(player, sel);
				else if (dir == -1)
					ScrollActions.interiorCollect(player, sel);
			} else if (payload.ctrl()) {
				if (dir == -1)
					ScrollActions.collectBorder(player, sel);
				else if (dir == 1)
					ScrollActions.placeBorder(player, sel);
			} else {
				if (dir == -1)
					ScrollActions.collect(player, sel);
				else if (dir == 1)
					ScrollActions.place(player, sel);
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(MiddleClickC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!player.getMainHandItem().is(ModItems.STONE_OF_MENDING))
				return;

			Selection sel = SelectionManager.get(player);
			if (sel == null || !sel.isComplete())
				return;
			if (!player.level().dimension().equals(sel.dimension()))
				return;

			ScrollActions.replace(player, sel);
		});

		ServerPlayNetworking.registerGlobalReceiver(SetNormalC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!player.getMainHandItem().is(ModItems.STONE_OF_MENDING))
				return;

			Selection sel = SelectionManager.get(player);
			if (sel == null || !sel.hasA())
				return;
			if (!player.level().dimension().equals(sel.dimension()))
				return;

			Direction newNormal = normalFromLook(player.getLookAngle());

			// A-only: just reorient (no box yet to pivot)
			if (!sel.isComplete()) {
				if (newNormal == sel.normal()) {
					ScrollActions.playSound(player, SoundEvents.AMETHYST_BLOCK_HIT, 0.3f);
					player.sendOverlayMessage(
							Component.literal("The stone already faces " + directionName(newNormal) + "."));
					return;
				}
				sel.setNormal(newNormal);
				sel.setFrontier(0);
				SelectionManager.sync(player);
				ScrollActions.playSound(player, SoundEvents.LODESTONE_COMPASS_LOCK, 0.5f);
				player.sendOverlayMessage(Component.literal("The stone now faces " + directionName(newNormal) + "."));
				return;
			}

			Direction oldNormal = sel.normal();
			if (newNormal == oldNormal) {
				ScrollActions.playSound(player, SoundEvents.AMETHYST_BLOCK_HIT, 0.3f);
				player.sendOverlayMessage(
						Component.literal("The stone already faces " + directionName(newNormal) + "."));
				return;
			}
			if (newNormal.getAxis() == oldNormal.getAxis()) {
				// Opposite direction on same axis: flip in place (box preserved, normal
				// flipped)
				sel.setNormal(newNormal);
				sel.setFrontier(0);
				SelectionManager.sync(player);
				ScrollActions.playSound(player, SoundEvents.LODESTONE_COMPASS_LOCK, 0.5f);
				player.sendOverlayMessage(
						Component.literal("The stone turns to face " + directionName(newNormal) + "."));
				return;
			}

			// Orthogonal direction with no stroke in progress: reorient whole box, preserve
			// shape
			if (sel.frontierOffset() == 0) {
				sel.setNormal(newNormal);
				SelectionManager.sync(player);
				ScrollActions.playSound(player, SoundEvents.LODESTONE_COMPASS_LOCK, 0.5f);
				player.sendOverlayMessage(Component.literal("The stone now faces " + directionName(newNormal) + "."));
				return;
			}

			// Orthogonal with stroke in progress: pivot at frontier face
			SelectionBox box = SelectionBox.from(sel.pointA(), sel.pointB(), oldNormal);
			int faceCoord = box.frontierBlock(sel.frontierOffset());

			int newAx = box.minX(), newAy = box.minY(), newAz = box.minZ();
			int newBx = box.maxX(), newBy = box.maxY(), newBz = box.maxZ();

			// Collapse old normal axis to frontier face
			switch (oldNormal.getAxis()) {
				case X -> {
					newAx = faceCoord;
					newBx = faceCoord;
				}
				case Y -> {
					newAy = faceCoord;
					newBy = faceCoord;
				}
				case Z -> {
					newAz = faceCoord;
					newBz = faceCoord;
				}
			}

			// Collapse new normal axis to min/max based on sign
			boolean positive = newNormal.getAxisDirection().getStep() > 0;
			switch (newNormal.getAxis()) {
				case X -> {
					int v = positive ? box.maxX() : box.minX();
					newAx = v;
					newBx = v;
				}
				case Y -> {
					int v = positive ? box.maxY() : box.minY();
					newAy = v;
					newBy = v;
				}
				case Z -> {
					int v = positive ? box.maxZ() : box.minZ();
					newAz = v;
					newBz = v;
				}
			}

			sel.setPoints(new BlockPos(newAx, newAy, newAz), new BlockPos(newBx, newBy, newBz));
			sel.setNormal(newNormal);
			sel.setFrontier(0);
			SelectionManager.sync(player);
			ScrollActions.playSound(player, SoundEvents.LODESTONE_COMPASS_LOCK, 0.5f);
			player.sendOverlayMessage(Component.literal("The stone pivots toward " + directionName(newNormal) + "."));
		});

		ServerPlayNetworking.registerGlobalReceiver(ClearSelectionC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!player.getMainHandItem().is(ModItems.STONE_OF_MENDING))
				return;

			Selection sel = SelectionManager.get(player);
			if (sel == null || !sel.hasA())
				return;

			SelectionManager.remove(player);
			ServerPlayNetworking.send(player, SelectionSyncPayload.from(new Selection()));
			ScrollActions.playSound(player, SoundEvents.AMETHYST_BLOCK_HIT, 0.3f);
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
			ScrollActions.playSound(serverPlayer, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f);
			serverPlayer.sendOverlayMessage(Component.literal("The stone remembers the first mark."));
			return InteractionResult.SUCCESS;
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer p = handler.getPlayer();
			SelectionManager.remove(p);
			topupTicks.remove(p.getUUID());
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			SelectionManager.tick(server);
			tickRepair(server);
			tickTopup(server);
		});

		LOGGER.info("Stone of Mending loaded");
	}

	private static Direction normalFromLook(Vec3 look) {
		double ax = Math.abs(look.x);
		double ay = Math.abs(look.y);
		double az = Math.abs(look.z);
		if (ax >= ay && ax >= az)
			return look.x >= 0 ? Direction.EAST : Direction.WEST;
		if (ay >= az)
			return look.y >= 0 ? Direction.UP : Direction.DOWN;
		return look.z >= 0 ? Direction.SOUTH : Direction.NORTH;
	}

	// Top-up: arrows + torches trickle while the stone is held, to keep
	// expedition basics from running dry. Per-player counter pauses when the
	// stone isn't in main hand and resumes from its last value — a 50-second
	// cadence would feel bad to reset on every hotbar flick.
	private static final int TOPUP_TICKS = 1000;
	private static final int ARROW_CAP = 24;
	private static final int TORCH_CAP = 16;
	private static final Map<UUID, Integer> topupTicks = new HashMap<>();

	private static void tickTopup(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.getMainHandItem().is(ModItems.STONE_OF_MENDING))
				continue;

			UUID id = player.getUUID();
			int t = topupTicks.getOrDefault(id, 0) + 1;
			if (t >= TOPUP_TICKS) {
				produceTopup(player);
				t = 0;
			}
			topupTicks.put(id, t);
		}
	}

	private static void produceTopup(ServerPlayer player) {
		Inventory inv = player.getInventory();
		int arrows = inv.countItem(Items.ARROW);
		int torches = inv.countItem(Items.TORCH);

		float arrowRatio = arrows / (float) ARROW_CAP;
		float torchRatio = torches / (float) TORCH_CAP;
		if (arrowRatio >= 1f && torchRatio >= 1f)
			return;

		// Tie (including empty-for-both) goes to arrows. Deterministic.
		boolean produceArrow = arrowRatio <= torchRatio;
		inv.add(new ItemStack(produceArrow ? Items.ARROW : Items.TORCH));
	}

	private static int repairTicks;

	private static void tickRepair(MinecraftServer server) {
		if (++repairTicks < 80)
			return;
		repairTicks = 0;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.getMainHandItem().is(ModItems.STONE_OF_MENDING))
				continue;

			ItemStack best = ItemStack.EMPTY;
			float bestRatio = 0;
			int bestDamage = 0;

			Inventory inv = player.getInventory();
			for (int i = 0; i < inv.getContainerSize(); i++) {
				ItemStack stack = inv.getItem(i);
				if (!stack.isDamaged())
					continue;

				int damage = stack.getDamageValue();
				float ratio = (float) damage / stack.getMaxDamage();
				if (ratio > bestRatio || (ratio == bestRatio && damage > bestDamage)) {
					best = stack;
					bestRatio = ratio;
					bestDamage = damage;
				}
			}

			if (!best.isEmpty()) {
				int repair = Math.max(1, best.getMaxDamage() / 100);
				best.setDamageValue(Math.max(0, best.getDamageValue() - repair));
			} else {
				mendNearbyStone(player);
			}
		}
	}

	// When everything is mended, the stone extends its attention outward and
	// mends the closest broken stone — cobblestone and cobbled deepslate — into
	// their whole forms. One block per 4s pulse. Experimental: blurs the
	// inventory/world line the other passives respect, but the name demands it.
	private static final int STONE_MEND_RADIUS = 4;

	private static void mendNearbyStone(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		double px = player.getX();
		double py = player.getY();
		double pz = player.getZ();
		BlockPos origin = player.blockPosition();

		BlockPos bestPos = null;
		Block bestResult = null;
		double bestDistSq = Double.MAX_VALUE;

		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dx = -STONE_MEND_RADIUS; dx <= STONE_MEND_RADIUS; dx++) {
			for (int dy = -STONE_MEND_RADIUS; dy <= STONE_MEND_RADIUS; dy++) {
				for (int dz = -STONE_MEND_RADIUS; dz <= STONE_MEND_RADIUS; dz++) {
					cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
					if (!level.isLoaded(cursor))
						continue;
					BlockState state = level.getBlockState(cursor);
					Block result;
					if (state.is(Blocks.COBBLESTONE))
						result = Blocks.STONE;
					else if (state.is(Blocks.COBBLED_DEEPSLATE))
						result = Blocks.DEEPSLATE;
					else
						continue;

					double distSq = cursor.distToCenterSqr(px, py, pz);
					if (distSq < bestDistSq) {
						bestDistSq = distSq;
						bestPos = cursor.immutable();
						bestResult = result;
					}
				}
			}
		}

		if (bestPos != null) {
			level.setBlock(bestPos, bestResult.defaultBlockState(),
					Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
		}
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
