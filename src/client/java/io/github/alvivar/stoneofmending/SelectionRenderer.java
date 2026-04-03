package io.github.alvivar.stoneofmending;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SelectionRenderer {

	private static final int COLOR_A_ONLY = ARGB.color(200, 100, 255, 100);
	private static final int COLOR_COMPLETE = ARGB.color(200, 100, 200, 255);

	public static void register() {
		LevelRenderEvents.AFTER_SOLID_FEATURES.register(SelectionRenderer::render);
	}

	private static void render(LevelRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		if (!mc.player.getMainHandItem().is(ModItems.STONE_OF_MENDING)) return;
		if (!ClientSelectionState.hasSelection()) return;

		// Don't render selections from other dimensions
		if (ClientSelectionState.dimension() != null && mc.level != null
				&& !mc.level.dimension().equals(ClientSelectionState.dimension())) {
			return;
		}

		BlockPos a = ClientSelectionState.pointA();
		if (a == null) return;

		BlockPos b = ClientSelectionState.pointB();
		boolean complete = ClientSelectionState.isComplete();

		int minX, minY, minZ, maxX, maxY, maxZ;
		if (b != null) {
			minX = Math.min(a.getX(), b.getX());
			minY = Math.min(a.getY(), b.getY());
			minZ = Math.min(a.getZ(), b.getZ());
			maxX = Math.max(a.getX(), b.getX()) + 1;
			maxY = Math.max(a.getY(), b.getY()) + 1;
			maxZ = Math.max(a.getZ(), b.getZ()) + 1;
		} else {
			minX = a.getX();
			minY = a.getY();
			minZ = a.getZ();
			maxX = a.getX() + 1;
			maxY = a.getY() + 1;
			maxZ = a.getZ() + 1;
		}

		// Shift to current frontier position along the normal
		if (complete) {
			Direction normal = ClientSelectionState.normal();
			int offset = ClientSelectionState.frontierOffset();
			minX += normal.getStepX() * offset;
			minY += normal.getStepY() * offset;
			minZ += normal.getStepZ() * offset;
			maxX += normal.getStepX() * offset;
			maxY += normal.getStepY() * offset;
			maxZ += normal.getStepZ() * offset;
		}

		VoxelShape shape = Shapes.box(0, 0, 0, maxX - minX, maxY - minY, maxZ - minZ);
		int color = complete ? COLOR_COMPLETE : COLOR_A_ONLY;

		Vec3 cam = context.levelState().cameraRenderState.pos;
		PoseStack poseStack = context.poseStack();
		VertexConsumer lines = context.bufferSource().getBuffer(RenderTypes.lines());

		poseStack.pushPose();
		ShapeRenderer.renderShape(
				poseStack, lines, shape,
				minX - cam.x, minY - cam.y, minZ - cam.z,
				color, 2.0f
		);
		poseStack.popPose();
	}
}
