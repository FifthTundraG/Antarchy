package com.craisinlord.antarchy.content.client.renderer;

import com.craisinlord.antarchy.content.block.entity.SeashellBlockEntity;
import com.craisinlord.antarchy.content.client.model.SeashellModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class SeashellRenderer extends GeoBlockRenderer<SeashellBlockEntity> {
    public SeashellRenderer(BlockEntityRendererProvider.Context context) {
        super(new SeashellModel());
    }

    @Override
    public void actuallyRender(
            PoseStack poseStack,
            SeashellBlockEntity animatable,
            BakedGeoModel model,
            net.minecraft.client.renderer.RenderType renderType,
            MultiBufferSource bufferSource,
            com.mojang.blaze3d.vertex.VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int colour
    ) {
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        if (isReRender) {
            return;
        }

        float yaw = -animatable.getBlockState().getValue(com.craisinlord.antarchy.content.block.SeashellBlock.FACING).toYRot();
        for (SeashellBlockEntity.DisplayedStack displayedStack : animatable.getDisplayedStacks()) {
            this.renderDisplayItem(poseStack, bufferSource, displayedStack.stack(), displayedStack.offset(), yaw, packedLight, packedOverlay);
        }
    }

    private void renderDisplayItem(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ItemStack stack,
            Vec3 localOffset,
            float yaw,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
        poseStack.translate(localOffset.x(), localOffset.y(), localOffset.z());
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(0.22F, 0.22F, 0.22F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                null,
                0
        );
        poseStack.popPose();
    }
}
