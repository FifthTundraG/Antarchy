package com.craisinlord.antarchy.content.client;

import com.craisinlord.antarchy.content.block.BigBushBlock;
import com.craisinlord.antarchy.content.block.BigBushPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BigBushRenderHelper {
    private BigBushRenderHelper() {
    }

    public static boolean isInsideBigBush(LivingEntity entity) {
        Level level = entity.level();
        return isBush(level, entity.blockPosition()) || isBush(level, BlockPos.containing(entity.getEyePosition()));
    }

    private static boolean isBush(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BigBushPartBlock) {
            return true;
        }
        return state.getBlock() instanceof BigBushBlock && state.getValue(BigBushBlock.AGE) > 0;
    }
}
