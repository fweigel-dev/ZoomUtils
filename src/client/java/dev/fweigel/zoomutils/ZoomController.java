package dev.fweigel.zoomutils;

public final class ZoomController {

    private static boolean zoomActive = false;

    /**
     * Volatile zoom level during an active zoom session.
     * Resets to the configured default each time the zoom key is pressed.
     * Modified by scroll wheel — never persisted to config.
     */
    private static double runtimeFov = 1.0;

    // Interpolated FOV values for smooth per-frame rendering.
    private static double prevFovMultiplier = 1.0;
    private static double fovMultiplier     = 1.0;

    private ZoomController() {}

    public static void tick(boolean keyDown) {
        prevFovMultiplier = fovMultiplier;

        if (keyDown && !zoomActive) {
            // Key just pressed — start from the configured default zoom, not the last scroll position.
            runtimeFov = ZoomUtilsConfig.getFovMultiplier();
        }
        zoomActive = keyDown;

        double target = zoomActive ? runtimeFov : 1.0;

        if (ZoomUtilsConfig.isSmoothZoom()) {
            fovMultiplier += (target - fovMultiplier) * 0.3;
            if (Math.abs(fovMultiplier - target) < 0.001) {
                fovMultiplier = target;
            }
        } else {
            fovMultiplier = target;
        }
    }

    public static double getInterpolated(float partialTick) {
        return prevFovMultiplier + (fovMultiplier - prevFovMultiplier) * partialTick;
    }

    public static boolean isZooming() {
        return zoomActive;
    }

    /**
     * Adjusts zoom via scroll while the zoom key is held.
     * Uses a multiplicative (logarithmic) step so each scroll tick feels the same at any zoom level.
     * Only modifies runtimeFov — the configured default zoom in settings is never affected.
     */
    public static void handleScroll(double scrollDelta) {
        // Each tick multiplies FOV by 0.85 (zoom in) or divides by 0.85 (zoom out),
        // giving ~18% change per step regardless of the current zoom level.
        double factor = Math.pow(0.85, scrollDelta);
        runtimeFov = Math.max(ZoomUtilsConfig.getMaxFovMultiplier(), Math.min(1.0, runtimeFov * factor));
    }
}
