package dev.fweigel.zoomutils;

import dev.fweigel.mobutils.core.client.ui.ModScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ZoomUtilsScreen extends ModScreen {

    // All preview PNGs are 320×180. Card displays at 96×54 (scale 0.3).

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

    // Smooth zoom ON:  11 frames from smooth_zoom.mp4     (smooth_zoom_01 … _11)
    // Smooth zoom OFF: 12 frames from no_smooth_zoom.mp4  (smooth_zoom_off_01 … _12)
    private static final Identifier[] SMOOTH_ON_FRAMES  = buildFrames("smooth_zoom_%02d.png",     11);
    private static final Identifier[] SMOOTH_OFF_FRAMES = buildFrames("smooth_zoom_off_%02d.png", 12);
    private static final long SMOOTH_FRAME_MS = 80L; // ~12 fps

    /** FOV presets — 1× means zoom key applies no initial zoom (scroll still works). */
    private static final double[] DEFAULT_PRESETS = {1.0, 0.5, 1.0 / 3.0, 0.25, 0.2, 0.125, 0.1};
    /** FOV presets — 1× means scroll is disabled. */
    private static final double[] MAX_PRESETS = {1.0, 0.5, 0.25, 0.125, 0.1, 0.05, 0.02, 0.01};

    // ── Animation pause state ──────────────────────────────────────────────────
    private boolean animPaused = false;
    private long    frozenAtMs = 0;

    // Stored in init() for use in mouseClicked
    private int animCardX, animCardY;

    // ── Buttons ────────────────────────────────────────────────────────────────
    private Button defaultZoomBtn;
    private Button maxZoomBtn;
    private Button smoothZoomBtn;

    public ZoomUtilsScreen() {
        super(Component.translatable("zoomutils.screen.title"));
    }

    @Override
    protected void init() {
        int cx   = this.width / 2;
        int colL = cx - BUTTON_WIDTH / 2;
        int colR = colL + CARD_W + COL_GAP;

        int row1  = 25;
        int btnY1 = row1 + CARD_PREV_H + CARD_BTN_GAP;
        int row2  = row1 + CARD_H + ROW_GAP;
        int btnY2 = row2 + CARD_PREV_H + CARD_BTN_GAP;

        animCardX = cx - CARD_W / 2;
        animCardY = row2;

        // ── Row 1: Default Zoom (left) | Max Zoom (right) ────────────────────
        defaultZoomBtn = addRenderableWidget(Button.builder(
                defaultZoomLabel(),
                b -> {
                    cycleDefaultZoom();
                    b.setMessage(defaultZoomLabel());
                    maxZoomBtn.setMessage(maxZoomLabel());
                }
        ).bounds(colL, btnY1, CARD_W, BUTTON_HEIGHT).build());

        maxZoomBtn = addRenderableWidget(Button.builder(
                maxZoomLabel(),
                b -> {
                    cycleMaxZoom();
                    b.setMessage(maxZoomLabel());
                    defaultZoomBtn.setMessage(defaultZoomLabel());
                }
        ).bounds(colR, btnY1, CARD_W, BUTTON_HEIGHT).build());

        // ── Row 2: Smooth Zoom toggle (full width) ────────────────────────────
        smoothZoomBtn = addRenderableWidget(Button.builder(
                smoothZoomLabel(),
                b -> {
                    ZoomUtilsConfig.setSmoothZoom(!ZoomUtilsConfig.isSmoothZoom());
                    b.setMessage(smoothZoomLabel());
                    ZoomUtilsConfig.save();
                }
        ).bounds(colL, btnY2, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        // ── Done ──────────────────────────────────────────────────────────────
        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                b -> this.onClose()
        ).bounds(colL, this.height - 28, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

        int cx   = this.width / 2;
        int colL = cx - BUTTON_WIDTH / 2;
        int colR = colL + CARD_W + COL_GAP;
        int row1 = 25;
        int row2 = row1 + CARD_H + ROW_GAP;

        // Row 1: static images that track the current setting value
        drawCardPreview(graphics, colL, row1, imageForFov(ZoomUtilsConfig.getFovMultiplier()));
        drawCardPreview(graphics, colR, row1, imageForFov(ZoomUtilsConfig.getMaxFovMultiplier()));

        // Row 2: animated smooth zoom preview, respects pause state
        Identifier[] frames = ZoomUtilsConfig.isSmoothZoom() ? SMOOTH_ON_FRAMES : SMOOTH_OFF_FRAMES;
        drawCardPreview(graphics, animCardX, row2, frames[currentFrame(frames)]);

        // Pause indicator — shown when paused or when hovering the card
        boolean hovering = mouseX >= animCardX && mouseX < animCardX + CARD_W
                        && mouseY >= row2       && mouseY < row2 + CARD_PREV_H;
        if (animPaused || hovering) {
            int ix = animCardX + CARD_W - 18;
            int iy = row2 + 2;
            graphics.fill(ix, iy, ix + 16, iy + 16, 0x99000000);
            graphics.centeredText(this.font, Component.literal(animPaused ? "▶" : "⏸"), ix + 8, iy + 4, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (!consumed && event.button() == 0
                && event.x() >= animCardX && event.x() < animCardX + CARD_W
                && event.y() >= animCardY && event.y() < animCardY + CARD_PREV_H) {
            animPaused = !animPaused;
            if (animPaused) frozenAtMs = System.currentTimeMillis();
            return true;
        }
        return super.mouseClicked(event, consumed);
    }

    // ── Animation helpers ──────────────────────────────────────────────────────

    private int currentFrame(Identifier[] frames) {
        long t = animPaused ? frozenAtMs : System.currentTimeMillis();
        return (int) ((t / SMOOTH_FRAME_MS) % frames.length);
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
