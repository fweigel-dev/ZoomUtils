package dev.fweigel.zoomutils.mixin;

import dev.fweigel.zoomutils.ZoomController;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts scroll events while the zoom key is held so the mouse wheel
 * adjusts the zoom level instead of switching hotbar slots or camera zoom.
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void zoomutils_onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (ZoomController.isZooming()) {
            ZoomController.handleScroll(yOffset);
            ci.cancel();
        }
    }
}
