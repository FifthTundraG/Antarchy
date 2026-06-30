package com.craisinlord.antarchy.fabric.client;

import com.craisinlord.antarchy.content.entity.HerculesBeetleEntity;
import com.craisinlord.antarchy.content.network.HerculesBeetleJumpInputPayload;
import com.craisinlord.antarchy.content.network.HerculesBeetleMountedAttackPayload;
import com.craisinlord.antarchy.content.network.HerculesBeetleMountedChargePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class HerculesBeetleClientHandler {
    private static final int MAX_CHARGE_TICKS = 40;

    private static boolean wasPressingJump;
    private static boolean wasPressingAttack;
    private static boolean wasCharging;
    private static int chargeTicks;

    private HerculesBeetleClientHandler() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) {
            resetAll();
            return;
        }

        if (!(player.getVehicle() instanceof HerculesBeetleEntity)) {
            resetAll();
            return;
        }

        boolean pressingJump = mc.options.keyJump.isDown();
        if (pressingJump != wasPressingJump) {
            ClientPlayNetworking.send(new HerculesBeetleJumpInputPayload(pressingJump));
            wasPressingJump = pressingJump;
        }

        boolean pressingAttack = mc.options.keyAttack.isDown();
        if (pressingAttack && !wasPressingAttack) {
            ClientPlayNetworking.send(new HerculesBeetleMountedAttackPayload());
        }
        wasPressingAttack = pressingAttack;

        boolean charging = AntarchyKeyBindings.isHerculesBeetleChargePressed();
        if (charging) {
            chargeTicks = Math.min(chargeTicks + 1, MAX_CHARGE_TICKS);
        } else if (wasCharging && chargeTicks > 0) {
            ClientPlayNetworking.send(new HerculesBeetleMountedChargePayload(chargeTicks));
            chargeTicks = 0;
        }
        wasCharging = charging;
    }

    private static void resetAll() {
        if (wasPressingJump) {
            ClientPlayNetworking.send(new HerculesBeetleJumpInputPayload(false));
            wasPressingJump = false;
        }
        wasPressingAttack = false;
        wasCharging = false;
        chargeTicks = 0;
    }
}
