package com.craisinlord.antarchy.neoforge.client;

import com.craisinlord.antarchy.Antarchy;
import com.craisinlord.antarchy.content.entity.RollyPollyEntity;
import com.craisinlord.antarchy.content.network.RollyPollyRollPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Antarchy.MODID, value = Dist.CLIENT)
public final class RollyPollyClientHandler {
    private static boolean wasPressingRoll;

    private RollyPollyClientHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || !(player.getVehicle() instanceof RollyPollyEntity)) {
            wasPressingRoll = false;
            return;
        }

        boolean pressingRoll = AntarchyKeyBindings.ROLLY_POLLY_ROLL.isDown();
        if (pressingRoll && !wasPressingRoll) {
            PacketDistributor.sendToServer(new RollyPollyRollPayload());
        }
        wasPressingRoll = pressingRoll;
    }
}
