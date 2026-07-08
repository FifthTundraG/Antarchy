package com.craisinlord.antarchy.content.worldgen;

import com.craisinlord.antarchy.content.block.VeryTallGrassBlock;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Places a single 3-tall {@link VeryTallGrassBlock} column, used for sparse overworld generation. */
public class VeryTallGrassWorldgenFeature extends Feature<NoneFeatureConfiguration> {
    public VeryTallGrassWorldgenFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        if (!level.getBlockState(pos).isAir()
                || !level.getBlockState(pos.above()).isAir()
                || !level.getBlockState(pos.above(2)).isAir()) {
            return false;
        }
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }
        VeryTallGrassBlock.placeAt(level, pos, Block.UPDATE_ALL);
        return true;
    }
}
