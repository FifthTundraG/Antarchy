package com.craisinlord.antarchy.content.worldgen.ocean;

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

public final class StarCoralPatchFeature extends Feature<NoneFeatureConfiguration> {
    public StarCoralPatchFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        Block starCoralBlock = AntarchyObjects.STAR_CORAL_BLOCK.get();
        Block starCoral = AntarchyObjects.STAR_CORAL.get();
        Block starCoralFan = AntarchyObjects.STAR_CORAL_FAN.get();

        int surfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        BlockPos floorPos = new BlockPos(origin.getX(), surfaceY - 1, origin.getZ());
        if (!level.getBlockState(floorPos).isSolid() || !level.getFluidState(floorPos.above()).is(FluidTags.WATER)) {
            return false;
        }

        int radius = 3 + random.nextInt(3);
        int targetPlacements = 8 + random.nextInt(9);
        int attempts = targetPlacements * 4;
        int placed = 0;

        for (int i = 0; i < attempts && placed < targetPlacements; i++) {
            int x = origin.getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = origin.getZ() + random.nextInt(radius * 2 + 1) - radius;
            int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
            BlockPos supportPos = new BlockPos(x, y - 1, z);
            BlockPos placePos = supportPos.above();

            if (!level.getBlockState(supportPos).isSolid()) {
                continue;
            }
            if (!level.getFluidState(placePos).is(FluidTags.WATER)) {
                continue;
            }
            if (!level.getBlockState(placePos).canBeReplaced()) {
                continue;
            }

            int roll = random.nextInt(10);
            BlockState toPlace;
            if (roll < 5) {
                toPlace = starCoralBlock.defaultBlockState();
            } else if (roll < 8) {
                toPlace = starCoral.defaultBlockState();
            } else {
                toPlace = starCoralFan.defaultBlockState();
            }

            level.setBlock(placePos, toPlace, 2);
            placed++;
        }

        return placed > 0;
    }
}
