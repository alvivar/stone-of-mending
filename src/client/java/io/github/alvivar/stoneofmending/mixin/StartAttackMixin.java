package io.github.alvivar.stoneofmending.mixin;

import io.github.alvivar.stoneofmending.ClearSelectionC2SPayload;
import io.github.alvivar.stoneofmending.ClientSelectionState;
import io.github.alvivar.stoneofmending.ModItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class StartAttackMixin {

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void stoneOfMending$clearOnMiss(CallbackInfoReturnable<Boolean> cir) {
		Minecraft mc = (Minecraft) (Object) this;
		if (mc.player == null) return;
		if (!mc.player.getMainHandItem().is(ModItems.STONE_OF_MENDING)) return;
		if (!ClientSelectionState.hasSelection()) return;
		if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.MISS) return;

		ClientPlayNetworking.send(new ClearSelectionC2SPayload());
		cir.setReturnValue(false);
	}
}
