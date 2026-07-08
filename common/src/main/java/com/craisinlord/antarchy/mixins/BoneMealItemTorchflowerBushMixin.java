package com.craisinlord.antarchy.mixins;

import com.craisinlord.antarchy.content.block.TorchflowerBushBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoneMealItem.class)
public abstract class BoneMealItemTorchflowerBushMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void antarchy$growTorchflowerBush(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(pos);
        if (!clickedState.is(Blocks.TORCHFLOWER)) {
            return;
        }

        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("antarchy", "torchflower_bush"));
        if (!(block instanceof TorchflowerBushBlock)) {
            return;
        }

        BlockState lowerState = block.defaultBlockState();
        if (!level.getBlockState(pos.above()).canBeReplaced() || !lowerState.canSurvive(level, pos)) {
            return;
        }

        if (!level.isClientSide) {
            DoublePlantBlock.placeAt(level, lowerState, pos, Block.UPDATE_ALL);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                ItemStack stack = context.getItemInHand();
                stack.shrink(1);
            }
            level.levelEvent(1505, pos, 15);
        }

        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
    }
}
