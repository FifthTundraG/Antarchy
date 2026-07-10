package com.craisinlord.antarchy.neoforge.network;

import com.craisinlord.antarchy.content.entity.RollyPollyEntity;
import com.craisinlord.antarchy.content.network.RollyPollyRollPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class RollyPollyNetworking {
    private RollyPollyNetworking() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(
                RollyPollyRollPayload.TYPE,
                RollyPollyRollPayload.STREAM_CODEC,
                RollyPollyNetworking::handleRollToggle
        );
    }

    private static void handleRollToggle(RollyPollyRollPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.getVehicle() instanceof RollyPollyEntity rollyPolly) {
                rollyPolly.handleRollToggle(player);
            }
        });
    }
}
