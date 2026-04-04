package io.github.alvivar.stoneofmending.mixin;

import io.github.alvivar.stoneofmending.ClientSelectionState;
import io.github.alvivar.stoneofmending.MiddleClickC2SPayload;
import io.github.alvivar.stoneofmending.ModItems;
import io.github.alvivar.stoneofmending.ScrollActionC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void stoneOfMending$interceptScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
		if (yOffset == 0) return;
		if (!isStoneActive()) return;

		int direction = yOffset > 0 ? 1 : -1;
		ClientPlayNetworking.send(new ScrollActionC2SPayload(direction));
		ci.cancel();
	}

	@Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
	private void stoneOfMending$interceptMiddleClick(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
		if (action != 1) return; // GLFW_PRESS
		if (buttonInfo.button() != 2) return; // middle button
		if (!isStoneActive()) return;

		ClientPlayNetworking.send(new MiddleClickC2SPayload());
		ci.cancel();
	}

	private boolean isStoneActive() {
		if (minecraft.screen != null) return false;
		if (minecraft.player == null) return false;
		if (!minecraft.player.getMainHandItem().is(ModItems.STONE_OF_MENDING)) return false;
		if (!ClientSelectionState.isComplete()) return false;
		if (ClientSelectionState.dimension() != null && minecraft.level != null
				&& !minecraft.level.dimension().equals(ClientSelectionState.dimension())) return false;
		return true;
	}
}
