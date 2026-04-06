package io.github.alvivar.stoneofmending;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class Selection {

	private @Nullable BlockPos pointA;
	private @Nullable BlockPos pointB;
	private @Nullable Direction normal;
	private @Nullable ResourceKey<Level> dimension;
	private int frontierOffset;

	public void markA(BlockPos pos, Direction face, ResourceKey<Level> dim) {
		this.pointA = pos;
		this.pointB = null;
		this.normal = face;
		this.dimension = dim;
		this.frontierOffset = 0;
	}

	public void markB(BlockPos pos) {
		this.pointB = pos;
	}

	public boolean hasA() {
		return pointA != null;
	}

	public boolean isComplete() {
		return pointA != null && pointB != null && normal != null;
	}

	public @Nullable BlockPos pointA() {
		return pointA;
	}

	public @Nullable BlockPos pointB() {
		return pointB;
	}

	public @Nullable Direction normal() {
		return normal;
	}

	public @Nullable ResourceKey<Level> dimension() {
		return dimension;
	}

	public int frontierOffset() {
		return frontierOffset;
	}

	public void setNormal(Direction direction) {
		this.normal = direction;
	}

	public void setFrontier(int offset) {
		this.frontierOffset = offset;
	}

}
