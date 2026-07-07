package com.craisinlord.antarchy.mixins;

import com.craisinlord.antarchy.content.block.VeryTallGrassBlock;
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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoneMealItem.class)
public abstract class BoneMealItemVeryTallGrassMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void antarchy$growVeryTallGrass(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(pos);
        if (!clickedState.is(Blocks.TALL_GRASS)) {
            return;
        }

        BlockPos basePos = clickedState.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockState baseState = level.getBlockState(basePos);
        BlockState middleState = level.getBlockState(basePos.above());
        if (!baseState.is(Blocks.TALL_GRASS)
                || baseState.getValue(DoublePlantBlock.HALF) != DoubleBlockHalf.LOWER
                || !middleState.is(Blocks.TALL_GRASS)
                || middleState.getValue(DoublePlantBlock.HALF) != DoubleBlockHalf.UPPER) {
            return;
        }

        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("antarchy", "very_tall_grass"));
        if (!(block instanceof VeryTallGrassBlock)) {
            return;
        }

        BlockState veryTallState = block.defaultBlockState();
        if (!level.getBlockState(basePos.above(2)).canBeReplaced()
                || !veryTallState.canSurvive(level, basePos)) {
            return;
        }

        if (!level.isClientSide) {
            VeryTallGrassBlock.placeAt(level, basePos, Block.UPDATE_ALL);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                ItemStack stack = context.getItemInHand();
                stack.shrink(1);
            }
            level.levelEvent(1505, basePos, 15);
        }

        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
    }
}
