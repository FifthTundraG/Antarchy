package com.craisinlord.antarchy.fabric.client;

import com.craisinlord.antarchy.content.entity.RollyPollyEntity;
import com.craisinlord.antarchy.content.network.RollyPollyRollPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class RollyPollyClientHandler {
    private static boolean wasPressingRoll;

    private RollyPollyClientHandler() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || !(player.getVehicle() instanceof RollyPollyEntity)) {
            wasPressingRoll = false;
            return;
        }

        boolean pressingRoll = AntarchyKeyBindings.ROLLY_POLLY_ROLL.isDown();
        if (pressingRoll && !wasPressingRoll) {
            ClientPlayNetworking.send(new RollyPollyRollPayload());
        }
        wasPressingRoll = pressingRoll;
    }
}
