package com.craisinlord.antarchy.mixins.client;

import com.craisinlord.antarchy.content.block.BigBushBlock;
import com.craisinlord.antarchy.content.block.BigBushPartBlock;
import com.craisinlord.antarchy.content.client.AlphaVertexConsumerProxy;
import com.craisinlord.antarchy.content.client.BigBushCameraHider;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BlockRenderDispatcher.class)
public abstract class BlockRenderDispatcherBigBushMixin {
    @ModifyVariable(method = "renderBatched", at = @At("HEAD"), argsOnly = true)
    private VertexConsumer antarchy$fadeBigBushAroundCamera(VertexConsumer consumer, BlockState state, BlockPos pos) {
        if ((state.getBlock() instanceof BigBushBlock || state.getBlock() instanceof BigBushPartBlock)
                && BigBushCameraHider.shouldFade(pos)) {
            return AlphaVertexConsumerProxy.wrap(consumer, 0.35F);
        }
        return consumer;
    }
}
