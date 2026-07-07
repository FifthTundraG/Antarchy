package com.craisinlord.antarchy.mixins.client;

import com.craisinlord.antarchy.content.client.BigBushRenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererBigBushMixin {
    @Unique
    private boolean antarchy$inBigBush;

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void antarchy$checkBigBush(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        this.antarchy$inBigBush = BigBushRenderHelper.isInsideBigBush(entity);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void antarchy$bigBushRenderType(LivingEntity entity, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        if (bodyVisible && !translucent && !glowing && BigBushRenderHelper.isInsideBigBush(entity)) {
            cir.setReturnValue(RenderType.itemEntityTranslucentCull(((EntityRenderer) (Object) this).getTextureLocation(entity)));
        }
    }

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void antarchy$hideBigBushNameTag(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (BigBushRenderHelper.isInsideBigBush(entity)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            index = 4
    )
    private int antarchy$bigBushAlpha(int color) {
        return this.antarchy$inBigBush && color == -1 ? 0x99FFFFFF : color;
    }
}
