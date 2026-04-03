package io.github.alvivar.stoneofmending;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ClientSelectionState {

	private static @Nullable BlockPos pointA;
	private static @Nullable BlockPos pointB;
	private static @Nullable Direction normal;
	private static @Nullable ResourceKey<Level> dimension;
	private static int frontierOffset;

	public static void update(SelectionSyncPayload payload) {
		pointA = payload.pointA();
		pointB = payload.pointB();
		normal = payload.normal();
		dimension = payload.dimension();
		frontierOffset = payload.frontierOffset();
	}

	public static void clear() {
		pointA = null;
		pointB = null;
		normal = null;
		dimension = null;
		frontierOffset = 0;
	}

	public static @Nullable BlockPos pointA() {
		return pointA;
	}

	public static @Nullable BlockPos pointB() {
		return pointB;
	}

	public static @Nullable Direction normal() {
		return normal;
	}

	public static @Nullable ResourceKey<Level> dimension() {
		return dimension;
	}

	public static int frontierOffset() {
		return frontierOffset;
	}

	public static boolean hasSelection() {
		return pointA != null;
	}

	public static boolean isComplete() {
		return pointA != null && pointB != null && normal != null;
	}
}
