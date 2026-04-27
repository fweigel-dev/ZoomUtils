package dev.fweigel.zoomutils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ZoomUtilsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("zoomutils.json");

    /** FOV multiplier when zoom key is pressed (1.0 = no zoom, 0.1 = 10x). Default 0.25 = 4x. */
    private static double fovMultiplier    = 0.25;
    /** Minimum FOV multiplier reachable via scroll (= maximum zoom power). Default 0.1 = 10x. */
    private static double maxFovMultiplier = 0.1;
    private static boolean smoothZoom = true;

    private ZoomUtilsConfig() {}

    public static double getFovMultiplier()    { return fovMultiplier; }
    public static double getMaxFovMultiplier() { return maxFovMultiplier; }
    public static boolean isSmoothZoom()       { return smoothZoom; }

    public static void setFovMultiplier(double value) {
        fovMultiplier = Math.max(0.05, Math.min(1.0, value));
    }

    public static void setMaxFovMultiplier(double value) {
        maxFovMultiplier = Math.max(0.01, Math.min(1.0, value));
    }

    public static void setSmoothZoom(boolean value) { smoothZoom = value; }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) return;
        try {
            SaveData data = GSON.fromJson(Files.readString(CONFIG_PATH), SaveData.class);
            if (data == null) return;
            if (data.fovMultiplier    != null) fovMultiplier    = Math.max(0.05, Math.min(1.0, data.fovMultiplier));
            if (data.maxFovMultiplier != null) maxFovMultiplier = Math.max(0.05, Math.min(1.0, data.maxFovMultiplier));
            if (data.smoothZoom       != null) smoothZoom       = data.smoothZoom;
        } catch (IOException ignored) {}
    }

    public static void save() {
        try {
            SaveData data = new SaveData();
            data.fovMultiplier    = fovMultiplier;
            data.maxFovMultiplier = maxFovMultiplier;
            data.smoothZoom       = smoothZoom;
            Files.writeString(CONFIG_PATH, GSON.toJson(data));
        } catch (IOException ignored) {}
    }

    private static class SaveData {
        Double  fovMultiplier;
        Double  maxFovMultiplier;
        Boolean smoothZoom;
    }
}
