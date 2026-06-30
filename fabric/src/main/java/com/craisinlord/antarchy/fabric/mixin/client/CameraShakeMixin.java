package com.craisinlord.antarchy.fabric.mixin.client;

import com.craisinlord.antarchy.content.entity.ToreterrorEntity;
import com.craisinlord.antarchy.content.entity.nightmare.NightmareEntity;
import com.craisinlord.antarchy.content.client.HerculesBeetleImpactShakeClientState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class CameraShakeMixin {
    private static final double MAX_SHAKE_RANGE = 48.0D;
    private static final int TORETERROR_SHAKE_TICKS = 25;
    private static final double NIGHTMARE_SHAKE_RANGE = 32.0D;

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "bobView", at = @At("TAIL"))
    private void applyCameraShake(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (minecraft.level == null || minecraft.player == null) return;

        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        float shakeStrength = 0.0F;

        for (ToreterrorEntity toreterror : minecraft.level.getEntitiesOfClass(
                ToreterrorEntity.class,
                minecraft.player.getBoundingBox().inflate(MAX_SHAKE_RANGE),
                t -> t.isAlive() && t.isJumpShaking()
        )) {
            double distance = Math.sqrt(cameraPos.distanceToSqr(toreterror.position().add(0, toreterror.getBbHeight() * 0.5, 0)));
            if (distance > MAX_SHAKE_RANGE) continue;
            shakeStrength += (float) ((1.0D - distance / MAX_SHAKE_RANGE) * 1.0F);
        }

        for (NightmareEntity nightmare : minecraft.level.getEntitiesOfClass(
                NightmareEntity.class,
                minecraft.player.getBoundingBox().inflate(NIGHTMARE_SHAKE_RANGE),
                entity -> entity.isAlive() && entity.isRoaring()
        )) {
            double distance = Math.sqrt(cameraPos.distanceToSqr(nightmare.position().add(0.0D, nightmare.getBbHeight() * 0.5D, 0.0D)));
            if (distance > NIGHTMARE_SHAKE_RANGE) continue;
            shakeStrength += (float) ((1.0D - distance / NIGHTMARE_SHAKE_RANGE) * 1.35D);
        }

        int beetleShakeTicks = HerculesBeetleImpactShakeClientState.getTicks();
        if (beetleShakeTicks > 0) {
            shakeStrength += (float) beetleShakeTicks / TORETERROR_SHAKE_TICKS * 2.0F;
        }

        if (shakeStrength <= 0.0F) return;

        float time = (float) (minecraft.player.tickCount + partialTick);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(time * 1.5F) * shakeStrength * 2.5F));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.cos(time * 1.8F) * shakeStrength * 2.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 2.2F) * shakeStrength * 1.5F));
    }
}
