package io.github.alvivar.stoneofmending;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SelectionRenderer {

	private static final int COLOR_A_ONLY = ARGB.color(200, 100, 255, 100);
	private static final int COLOR_BOX = ARGB.color(80, 100, 200, 255);
	private static final int COLOR_FRONTIER = ARGB.color(220, 100, 255, 200);

	public static void register() {
		LevelRenderEvents.AFTER_SOLID_FEATURES.register(SelectionRenderer::render);
	}

	private static void render(LevelRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		if (!mc.player.getMainHandItem().is(ModItems.STONE_OF_MENDING)) return;
		if (!ClientSelectionState.hasSelection()) return;

		if (ClientSelectionState.dimension() != null && mc.level != null
				&& !mc.level.dimension().equals(ClientSelectionState.dimension())) {
			return;
		}

		BlockPos a = ClientSelectionState.pointA();
		if (a == null) return;

		Vec3 cam = context.levelState().cameraRenderState.pos;
		PoseStack poseStack = context.poseStack();
		VertexConsumer lines = context.bufferSource().getBuffer(RenderTypes.lines());

		BlockPos b = ClientSelectionState.pointB();

		if (b == null) {
			// A only — single block highlight
			drawBox(poseStack, lines, cam,
					a.getX(), a.getY(), a.getZ(),
					a.getX() + 1, a.getY() + 1, a.getZ() + 1,
					COLOR_A_ONLY);
			return;
		}

		// Complete selection — dim box for full volume, bright slice for frontier
		SelectionBox box = SelectionBox.from(a, b, ClientSelectionState.normal());
		int offset = ClientSelectionState.frontierOffset();

		// Dim outline of the full 3D selection
		drawBox(poseStack, lines, cam,
				box.minX(), box.minY(), box.minZ(),
				box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1,
				COLOR_BOX);

		// Bright frontier slice
		int fb = box.frontierBlock(offset);
		int sMinX, sMinY, sMinZ, sMaxX, sMaxY, sMaxZ;
		switch (box.normal().getAxis()) {
			case X -> { sMinX = fb; sMaxX = fb + 1; sMinY = box.minY(); sMaxY = box.maxY() + 1; sMinZ = box.minZ(); sMaxZ = box.maxZ() + 1; }
			case Y -> { sMinX = box.minX(); sMaxX = box.maxX() + 1; sMinY = fb; sMaxY = fb + 1; sMinZ = box.minZ(); sMaxZ = box.maxZ() + 1; }
			case Z -> { sMinX = box.minX(); sMaxX = box.maxX() + 1; sMinY = box.minY(); sMaxY = box.maxY() + 1; sMinZ = fb; sMaxZ = fb + 1; }
			default -> { return; }
		}

		drawBox(poseStack, lines, cam,
				sMinX, sMinY, sMinZ, sMaxX, sMaxY, sMaxZ,
				COLOR_FRONTIER);
	}

	private static void drawBox(PoseStack poseStack, VertexConsumer lines, Vec3 cam,
			int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int color) {
		VoxelShape shape = Shapes.box(0, 0, 0, maxX - minX, maxY - minY, maxZ - minZ);
		poseStack.pushPose();
		ShapeRenderer.renderShape(
				poseStack, lines, shape,
				minX - cam.x, minY - cam.y, minZ - cam.z,
				color, 2.0f
		);
		poseStack.popPose();
	}
}
