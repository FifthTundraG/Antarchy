package com.craisinlord.antarchy.content.worldgen.elythia;

import com.craisinlord.antarchy.content.AntarchyObjects;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class GiantLilyPadPatchFeature extends Feature<NoneFeatureConfiguration> {
    public GiantLilyPadPatchFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        boolean placedAny = false;

        for (int i = 0; i < 12; i++) {
            int anchorX = origin.getX() + random.nextInt(17) - 8;
            int anchorZ = origin.getZ() + random.nextInt(17) - 8;
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, anchorX, anchorZ) - 1;
            BlockPos anchorFluidPos = new BlockPos(anchorX, surfaceY, anchorZ);
            if (!level.getFluidState(anchorFluidPos).is(FluidTags.WATER) || !level.getFluidState(anchorFluidPos).isSource()) {
                continue;
            }

            int size = rollFootprintSize(random);
            List<BlockPos> footprint = new ArrayList<>(size * size);
            boolean valid = true;
            for (int row = 0; row < size && valid; row++) {
                for (int column = 0; column < size; column++) {
                    BlockPos fluidPos = new BlockPos(anchorX + column, surfaceY, anchorZ + row);
                    if (!level.getFluidState(fluidPos).is(FluidTags.WATER) || !level.getFluidState(fluidPos).isSource()) {
                        valid = false;
                        break;
                    }

                    BlockPos padPos = fluidPos.above();
                    if (!level.isEmptyBlock(padPos)) {
                        valid = false;
                        break;
                    }

                    footprint.add(padPos);
                }
            }

            if (!valid) {
                continue;
            }

            Block giantLilyPad = AntarchyObjects.GIANT_LILY_PAD.get();
            BlockState padState = giantLilyPad.defaultBlockState();
            for (BlockPos padPos : footprint) {
                level.setBlock(padPos, padState, Block.UPDATE_ALL);
            }

            placedAny = true;

            if (size == 3 && random.nextFloat() < 0.5F) {
                BlockPos centerPos = new BlockPos(anchorX + 1, surfaceY, anchorZ + 1).above();
                placeLotus(level, centerPos.above());
            }
        }

        return placedAny;
    }

    private static int rollFootprintSize(RandomSource random) {
        float roll = random.nextFloat();
        if (roll < 0.5F) {
            return 1;
        }

        return roll < 0.8F ? 2 : 3;
    }

    private void placeLotus(WorldGenLevel level, BlockPos pos) {
        if (!level.isEmptyBlock(pos)) {
            return;
        }

        BlockState lotusState = AntarchyObjects.LOTUS.get().defaultBlockState();
        if (lotusState.canSurvive(level, pos)) {
            level.setBlock(pos, lotusState, Block.UPDATE_ALL);
        }
    }
}
