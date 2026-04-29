package dev.fweigel.zoomutils;

import dev.fweigel.mobutils.core.client.ui.ModOptionsList;
import dev.fweigel.mobutils.core.client.ui.ModOptionsList.CardSpec;
import dev.fweigel.mobutils.core.client.ui.ModSettingsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ZoomUtilsScreen extends ModSettingsScreen {

    private static final Identifier IMG_1X   = id("zoom_1x.png");
    private static final Identifier IMG_2X   = id("zoom_2x.png");
    private static final Identifier IMG_3X   = id("zoom_3x.png");
    private static final Identifier IMG_4X   = id("zoom_4x.png");
    private static final Identifier IMG_5X   = id("zoom_5x.png");
    private static final Identifier IMG_8X   = id("zoom_8x.png");
    private static final Identifier IMG_10X  = id("zoom_10x.png");
    private static final Identifier IMG_20X  = id("zoom_20x.png");
    private static final Identifier IMG_50X  = id("zoom_50x.png");
    private static final Identifier IMG_100X = id("zoom_100x.png");

    private static final Identifier[] SMOOTH_ON_FRAMES  = buildFrames("smooth_zoom_%02d.png",     11);
    private static final Identifier[] SMOOTH_OFF_FRAMES = buildFrames("smooth_zoom_off_%02d.png", 12);
    private static final long SMOOTH_FRAME_MS = 80L;

    private static final double[] DEFAULT_PRESETS = {1.0, 0.5, 1.0 / 3.0, 0.25, 0.2, 0.125, 0.1};
    private static final double[] MAX_PRESETS     = {1.0, 0.5, 0.25, 0.125, 0.1, 0.05, 0.02, 0.01};

    public ZoomUtilsScreen() {
        super(Component.translatable("zoomutils.screen.title"));
    }

    @Override
    protected void addOptions(ModOptionsList list) {
        // ── Row 1: Default Zoom (left) | Max Zoom (right) ────────────────────
        Button[] defaultRef = new Button[1], maxRef = new Button[1];
        defaultRef[0] = Button.builder(defaultZoomLabel(), b -> {
            cycleDefaultZoom();
            defaultRef[0].setMessage(defaultZoomLabel());
            maxRef[0].setMessage(maxZoomLabel());
        }).bounds(0, 0, CARD_W, BUTTON_HEIGHT).build();
        maxRef[0] = Button.builder(maxZoomLabel(), b -> {
            cycleMaxZoom();
            maxRef[0].setMessage(maxZoomLabel());
            defaultRef[0].setMessage(defaultZoomLabel());
        }).bounds(0, 0, CARD_W, BUTTON_HEIGHT).build();

        list.addSplitCard(
            CardSpec.image(() -> imageForFov(ZoomUtilsConfig.getFovMultiplier())),
            defaultRef[0],
            CardSpec.image(() -> imageForFov(ZoomUtilsConfig.getMaxFovMultiplier())),
            maxRef[0]
        );

        // ── Row 2: Smooth Zoom animated preview (pausable) ────────────────────
        list.addSingleCard(
            CardSpec.animated(
                () -> ZoomUtilsConfig.isSmoothZoom() ? SMOOTH_ON_FRAMES : SMOOTH_OFF_FRAMES,
                SMOOTH_FRAME_MS, true),
            buildWideButton(this::smoothZoomLabel, () -> {
                ZoomUtilsConfig.setSmoothZoom(!ZoomUtilsConfig.isSmoothZoom());
                ZoomUtilsConfig.save();
            })
        );
    }

    // ── Image selection ────────────────────────────────────────────────────────

    private static Identifier imageForFov(double fov) {
        int factor = (int) Math.round(1.0 / fov);
        return switch (factor) {
            case 2   -> IMG_2X;
            case 3   -> IMG_3X;
            case 4   -> IMG_4X;
            case 5   -> IMG_5X;
            case 8   -> IMG_8X;
            case 10  -> IMG_10X;
            case 20  -> IMG_20X;
            case 50  -> IMG_50X;
            case 100 -> IMG_100X;
            default  -> IMG_1X;
        };
    }

    // ── Label helpers ──────────────────────────────────────────────────────────

    private Component defaultZoomLabel() {
        int factor = (int) Math.round(1.0 / ZoomUtilsConfig.getFovMultiplier());
        return Component.translatable("zoomutils.screen.default_zoom", factor + "x");
    }

    private Component maxZoomLabel() {
        int factor = (int) Math.round(1.0 / ZoomUtilsConfig.getMaxFovMultiplier());
        return Component.translatable("zoomutils.screen.max_zoom", factor + "x");
    }

    private Component smoothZoomLabel() {
        String state = Component.translatable(
                ZoomUtilsConfig.isSmoothZoom() ? "zoomutils.state.on" : "zoomutils.state.off"
        ).getString();
        return Component.translatable("zoomutils.screen.smooth_zoom", state);
    }

    // ── Cycle helpers ──────────────────────────────────────────────────────────

    private static void cycleDefaultZoom() {
        double current = ZoomUtilsConfig.getFovMultiplier();
        int nextIndex = (closestIndex(DEFAULT_PRESETS, current) + 1) % DEFAULT_PRESETS.length;
        double newDefault = DEFAULT_PRESETS[nextIndex];
        ZoomUtilsConfig.setFovMultiplier(newDefault);
        if (ZoomUtilsConfig.getMaxFovMultiplier() > newDefault) {
            ZoomUtilsConfig.setMaxFovMultiplier(newDefault);
        }
        ZoomUtilsConfig.save();
    }

    private static void cycleMaxZoom() {
        double current = ZoomUtilsConfig.getMaxFovMultiplier();
        int nextIndex = (closestIndex(MAX_PRESETS, current) + 1) % MAX_PRESETS.length;
        double newMax = MAX_PRESETS[nextIndex];
        ZoomUtilsConfig.setMaxFovMultiplier(newMax);
        if (ZoomUtilsConfig.getFovMultiplier() < newMax) {
            ZoomUtilsConfig.setFovMultiplier(newMax);
        }
        ZoomUtilsConfig.save();
    }

    private static int closestIndex(double[] presets, double value) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < presets.length; i++) {
            double dist = Math.abs(presets[i] - value);
            if (dist < bestDist) { bestDist = dist; best = i; }
        }
        return best;
    }

    // ── Utilities ──────────────────────────────────────────────────────────────

    private static Identifier id(String path) {
        return Identifier.parse("zoomutils:textures/gui/preview/" + path);
    }

    private static Identifier[] buildFrames(String pattern, int count) {
        Identifier[] frames = new Identifier[count];
        for (int i = 0; i < count; i++) frames[i] = id(String.format(pattern, i + 1));
        return frames;
    }
}
