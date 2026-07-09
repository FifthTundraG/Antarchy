package com.craisinlord.apple_cows.fabric.client;

import com.craisinlord.apple_cows.content.client.renderer.AppleCowRenderer;
import com.craisinlord.apple_cows.fabric.AppleCowsFabricContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class AppleCowsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(AppleCowsFabricContent.APPLE_COW, AppleCowRenderer::new);
        EntityRendererRegistry.register(AppleCowsFabricContent.GOLDEN_APPLE_COW, AppleCowRenderer::new);
        EntityRendererRegistry.register(AppleCowsFabricContent.ENCHANTED_GOLDEN_APPLE_COW, AppleCowRenderer::new);
    }
}
