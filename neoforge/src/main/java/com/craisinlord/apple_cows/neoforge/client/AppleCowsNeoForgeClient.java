package com.craisinlord.apple_cows.neoforge.client;

import com.craisinlord.apple_cows.AppleCows;
import com.craisinlord.apple_cows.content.client.renderer.AppleCowRenderer;
import com.craisinlord.apple_cows.neoforge.registry.AppleCowsNeoforgeEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = AppleCows.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AppleCowsNeoForgeClient {
    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AppleCowsNeoforgeEntities.APPLE_COW.get(), AppleCowRenderer::new);
        event.registerEntityRenderer(AppleCowsNeoforgeEntities.GOLDEN_APPLE_COW.get(), AppleCowRenderer::new);
        event.registerEntityRenderer(AppleCowsNeoforgeEntities.ENCHANTED_GOLDEN_APPLE_COW.get(), AppleCowRenderer::new);
    }
}
