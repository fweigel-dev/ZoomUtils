package dev.fweigel.zoomutils.mixin;

import dev.fweigel.zoomutils.ZoomController;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Scales the computed FOV by the current zoom multiplier.
 * Targets Camera.calculateFov which is called every update with the partial tick,
 * giving us smooth per-frame interpolation for free.
 */
@Mixin(Camera.class)
public class CameraMixin {

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void zoomutils_calculateFov(float partialTick, CallbackInfoReturnable<Float> cir) {
        float multiplier = (float) ZoomController.getInterpolated(partialTick);
        if (Math.abs(multiplier - 1.0f) > 0.001f) {
            cir.setReturnValue(cir.getReturnValue() * multiplier);
        }
    }
}
