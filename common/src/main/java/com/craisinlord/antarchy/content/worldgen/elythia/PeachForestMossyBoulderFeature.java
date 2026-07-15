package com.craisinlord.antarchy.content.worldgen.elythia;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class PeachForestMossyBoulderFeature extends Feature<NoneFeatureConfiguration> {
    public PeachForestMossyBoulderFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockPos surfacePos = new BlockPos(origin.getX(), level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ()) - 1, origin.getZ());
        if (!isSoil(level.getBlockState(surfacePos))) {
            return false;
        }

        BlockPos base = surfacePos.below(random.nextInt(2));
        int height = 6 + random.nextInt(5);
        int baseRadius = 2 * (2 + random.nextInt(2));
        boolean placed = false;

        int driftX = 0;
        int driftZ = 0;
        int topRadius = 1;
        BlockPos topCenter = base;
        for (int y = 0; y < height; y++) {
            double heightFraction = (double) y / (height - 1);
            double taper = 1.0D - heightFraction * 0.7D;
            int radius = Math.max(1, Math.round((float) (baseRadius * taper)));

            driftX = Mth.clamp(driftX + random.nextInt(3) - 1, -1, 1);
            driftZ = Mth.clamp(driftZ + random.nextInt(3) - 1, -1, 1);
            BlockPos levelCenter = base.above(y).offset(driftX, 0, driftZ);

            placed |= this.placeDisk(level, levelCenter, radius, heightFraction, random);
            topCenter = levelCenter;
            topRadius = radius;
        }

        if (placed) {
            this.softCapWithSlabs(level, topCenter, topRadius, random);
        }

        return placed;
    }

    private boolean placeDisk(WorldGenLevel level, BlockPos center, int radius, double heightFraction, RandomSource random) {
        boolean placed = false;
        double rr = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = (x * x + z * z) / Math.max(1.0D, rr);
                if (distance > 1.0D + random.nextDouble() * 0.3D) {
                    continue;
                }

                BlockPos targetPos = center.offset(x, 0, z);
                BlockState targetState = level.getBlockState(targetPos);
                if (targetState.isAir() || targetState.canBeReplaced() || isSoil(targetState)) {
                    level.setBlock(targetPos, pickBlockState(heightFraction, random), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    private void softCapWithSlabs(WorldGenLevel level, BlockPos center, int radius, RandomSource random) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos topPos = center.offset(x, 0, z);
                while (topPos.getY() < level.getMaxBuildHeight() - 1 && isBoulderBlock(level.getBlockState(topPos.above()))) {
                    topPos = topPos.above();
                }

                BlockState topState = level.getBlockState(topPos);
                if (!isBoulderBlock(topState)) {
                    continue;
                }

                BlockPos slabPos = topPos.above();
                if (!level.isEmptyBlock(slabPos) || random.nextFloat() > 0.25F) {
                    continue;
                }

                int openSides = 0;
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    if (!isBoulderBlock(level.getBlockState(topPos.relative(direction)))) {
                        openSides++;
                    }
                }

                if (openSides == 0) {
                    continue;
                }

                BlockState slabState = (random.nextFloat() < 0.55F ? Blocks.MOSSY_COBBLESTONE_SLAB : Blocks.COBBLESTONE_SLAB)
                        .defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
                level.setBlock(slabPos, slabState, 2);
            }
        }
    }

    private static BlockState pickBlockState(double heightFraction, RandomSource random) {
        if (heightFraction < 0.35D) {
            return Blocks.TUFF.defaultBlockState();
        }

        return random.nextFloat() < 0.55F ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
    }

    private static boolean isBoulderBlock(BlockState state) {
        return state.is(Blocks.TUFF) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.MOSSY_COBBLESTONE);
    }

    private static boolean isSoil(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.ROOTED_DIRT);
    }
}
