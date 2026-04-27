package dev.fweigel.zoomutils;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ZoomUtilsClient implements ClientModInitializer {

    public static KeyMapping zoomKey;
    private static KeyMapping configKey;

    @Override
    public void onInitializeClient() {
        ZoomUtilsConfig.load();

        // Register both keys under one shared category — calling ConfigKeyHelper twice
        // with the same modId would try to register the same category twice and crash.
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("zoomutils", "general"));
        zoomKey   = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.zoomutils.zoom",   GLFW.GLFW_KEY_C,       category));
        configKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.zoomutils.config", GLFW.GLFW_KEY_UNKNOWN, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ZoomController.tick(zoomKey.isDown());

            while (configKey.consumeClick()) {
                client.setScreen(new ZoomUtilsScreen());
            }
        });
    }
}
