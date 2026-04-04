package io.github.alvivar.stoneofmending;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Computed geometry for a complete selection. Derived from A, B, and the face normal.
 * Shared by renderer, collection, and placement to keep the math in one place.
 */
public record SelectionBox(
		int minX, int minY, int minZ,
		int maxX, int maxY, int maxZ,
		Direction normal
) {

	public static SelectionBox from(BlockPos a, BlockPos b, Direction normal) {
		return new SelectionBox(
				Math.min(a.getX(), b.getX()),
				Math.min(a.getY(), b.getY()),
				Math.min(a.getZ(), b.getZ()),
				Math.max(a.getX(), b.getX()),
				Math.max(a.getY(), b.getY()),
				Math.max(a.getZ(), b.getZ()),
				normal
		);
	}

	/** The block coordinate on the normal axis where the face (outermost side) is. */
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
}
