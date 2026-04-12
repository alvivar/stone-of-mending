package io.github.alvivar.stoneofmending;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Computed geometry for a complete selection. Derived from A, B, and the face
 * normal.
 * Shared by renderer, collection, and placement to keep the math in one place.
 */
public record SelectionBox(
		int minX, int minY, int minZ,
		int maxX, int maxY, int maxZ,
		Direction normal) {

	public static SelectionBox from(BlockPos a, BlockPos b, Direction normal) {
		return new SelectionBox(
				Math.min(a.getX(), b.getX()),
				Math.min(a.getY(), b.getY()),
				Math.min(a.getZ(), b.getZ()),
				Math.max(a.getX(), b.getX()),
				Math.max(a.getY(), b.getY()),
				Math.max(a.getZ(), b.getZ()),
				normal);
	}

	/**
	 * The block coordinate on the normal axis where the face (outermost side) is.
	 */
	public int faceBlock() {
		int step = normal.getAxisDirection().getStep();
		return switch (normal.getAxis()) {
			case X -> step > 0 ? maxX : minX;
			case Y -> step > 0 ? maxY : minY;
			case Z -> step > 0 ? maxZ : minZ;
		};
	}

	/** The block coordinate on the normal axis for a given frontier offset. */
	public int frontierBlock(int offset) {
		int step = normal.getAxisDirection().getStep();
		return faceBlock() - offset * step;
	}

	/** Number of slices along the normal axis within the selected box. */
	public int depth() {
		return switch (normal.getAxis()) {
			case X -> maxX - minX + 1;
			case Y -> maxY - minY + 1;
			case Z -> maxZ - minZ + 1;
		};
	}

	/** Iterate all block positions in a frontier slice at the given offset. */
	public Iterable<BlockPos> slicePositions(int offset) {
		int fb = frontierBlock(offset);
		return switch (normal.getAxis()) {
			case X -> BlockPos.betweenClosed(fb, minY, minZ, fb, maxY, maxZ);
			case Y -> BlockPos.betweenClosed(minX, fb, minZ, maxX, fb, maxZ);
			case Z -> BlockPos.betweenClosed(minX, minY, fb, maxX, maxY, fb);
		};
	}

	/**
	 * Iterate only the perimeter positions of a frontier slice. Scales with
	 * perimeter, not area.
	 */
	public List<BlockPos> borderPositions(int offset) {
		int fb = frontierBlock(offset);

		// The two axes of the 2D slice
		int minU, maxU, minV, maxV;
		switch (normal.getAxis()) {
			case X -> {
				minU = minY;
				maxU = maxY;
				minV = minZ;
				maxV = maxZ;
			}
			case Y -> {
				minU = minX;
				maxU = maxX;
				minV = minZ;
				maxV = maxZ;
			}
			default -> {
				minU = minX;
				maxU = maxX;
				minV = minY;
				maxV = maxY;
			}
		}

		int uSize = maxU - minU + 1;
		int vSize = maxV - minV + 1;

		// If either dimension <= 2, all positions are border
		if (uSize <= 2 || vSize <= 2) {
			List<BlockPos> all = new ArrayList<>(uSize * vSize);
			for (BlockPos pos : slicePositions(offset)) {
				all.add(pos.immutable());
			}
			return all;
		}

		int perimeter = 2 * (uSize + vSize) - 4;
		List<BlockPos> border = new ArrayList<>(perimeter);

		// Top and bottom edges (full rows)
		for (int v = minV; v <= maxV; v++) {
			addBorderPos(border, fb, minU, v);
			addBorderPos(border, fb, maxU, v);
		}
		// Left and right edges (interior rows only)
		for (int u = minU + 1; u < maxU; u++) {
			addBorderPos(border, fb, u, minV);
			addBorderPos(border, fb, u, maxV);
		}
		return border;
	}

	private void addBorderPos(List<BlockPos> list, int fb, int u, int v) {
		switch (normal.getAxis()) {
			case X -> list.add(new BlockPos(fb, u, v));
			case Y -> list.add(new BlockPos(u, fb, v));
			case Z -> list.add(new BlockPos(u, v, fb));
		}
	}
}
