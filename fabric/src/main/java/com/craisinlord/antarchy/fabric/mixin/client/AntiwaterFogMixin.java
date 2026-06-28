package com.craisinlord.antarchy.fabric.mixin.client;

import com.craisinlord.antarchy.content.fluid.BileLiquidBlock;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class AntiwaterFogMixin {
    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void antarchy$overrideBileColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, CallbackInfo ci) {
        if (!antarchy$isInBile(camera)) {
            return;
        }

        RenderSystem.clearColor(0.38F, 0.42F, 0.10F, 0.0F);
    }

    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void antarchy$overrideBileFog(Camera camera, FogRenderer.FogMode mode, float renderDistance, boolean thickFog, float partialTick, CallbackInfo ci) {
        if (!antarchy$isInBile(camera)) {
            return;
        }

        RenderSystem.setShaderFogStart(0.25F);
        RenderSystem.setShaderFogEnd(Math.min(renderDistance, 4.0F));
        RenderSystem.setShaderFogShape(FogShape.CYLINDER);
    }

    private static boolean antarchy$isInBile(Camera camera) {
        Entity entity = camera.getEntity();
        if (entity == null || entity.level() == null) {
            return false;
        }

        BlockPos pos = BlockPos.containing(camera.getPosition());
        FluidState fluidState = entity.level().getFluidState(pos);
        return BileLiquidBlock.isBile(fluidState);
    }
}
