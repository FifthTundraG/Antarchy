package com.craisinlord.antarchy.content.client.renderer;

import com.craisinlord.antarchy.content.client.model.GlimmerModel;
import com.craisinlord.antarchy.content.entity.GlimmerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class GlimmerRenderer extends GeoEntityRenderer<GlimmerEntity> {
    private static final float SHADOW_RADIUS = 0.5F;

    public GlimmerRenderer(EntityRendererProvider.Context context) {
        super(context, new GlimmerModel());
        this.shadowRadius = SHADOW_RADIUS;
        this.addRenderLayer(new GlimmerCrossfadeLayer(this));
        this.addRenderLayer(new GlimmerEmissiveLayer(this));
    }

    @Override
    public RenderType getRenderType(GlimmerEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void preRender(PoseStack poseStack, GlimmerEntity animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        this.shadowRadius = SHADOW_RADIUS;
    }

    private static final class GlimmerCrossfadeLayer extends GeoRenderLayer<GlimmerEntity> {
        private GlimmerCrossfadeLayer(GeoEntityRenderer<GlimmerEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, GlimmerEntity animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType,
                           MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick,
                           int packedLight, int packedOverlay) {
            float glowBlend = animatable.getGlowBlend(partialTick);
            if (glowBlend <= 0.0F || glowBlend >= 1.0F) {
                return;
            }

            boolean baseGlowing = glowBlend >= 0.5F;
            float overlayAlpha = baseGlowing ? 1.0F - glowBlend : glowBlend;
            if (overlayAlpha <= 0.0F) {
                return;
            }

            ResourceLocation overlayTexture = GlimmerModel.textureFor(animatable, !baseGlowing);
            RenderType overlayType = RenderType.entityTranslucent(overlayTexture);
            VertexConsumer overlayBuffer = bufferSource.getBuffer(overlayType);
            int color = ((Math.max(0, Math.min(255, Math.round(overlayAlpha * 255.0F))) & 0xFF) << 24) | 0x00FFFFFF;
            this.getRenderer().reRender(
                    bakedModel,
                    poseStack,
                    bufferSource,
                    animatable,
                    overlayType,
                    overlayBuffer,
                    partialTick,
                    packedLight,
                    packedOverlay,
                    color
            );
        }
    }

    private static final class GlimmerEmissiveLayer extends GeoRenderLayer<GlimmerEntity> {
        private GlimmerEmissiveLayer(GeoEntityRenderer<GlimmerEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, GlimmerEntity animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType,
                           MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick,
                           int packedLight, int packedOverlay) {
            if (!animatable.isGlowingState()) {
                return;
            }

            ResourceLocation texture = animatable.isBaby() ? GlimmerModel.BABY_EMISSIVE : GlimmerModel.ADULT_EMISSIVE;
            RenderType emissiveType = RenderType.eyes(texture);
            VertexConsumer emissiveBuffer = bufferSource.getBuffer(emissiveType);
            this.getRenderer().reRender(
                    bakedModel,
                    poseStack,
                    bufferSource,
                    animatable,
                    emissiveType,
                    emissiveBuffer,
                    partialTick,
                    0xF000F0,
                    OverlayTexture.NO_OVERLAY,
                    0xFFFFFFFF
            );
        }
    }
}
