package com.craisinlord.antarchy.content.worldgen;

import com.craisinlord.antarchy.content.block.BigBushBlock;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/** Places a {@link BigBushBlock} at a fixed growth stage (age), used for sparse overworld generation. */
public class BigBushWorldgenFeature extends Feature<NoneFeatureConfiguration> {
    private final int age;

    public BigBushWorldgenFeature(Codec<NoneFeatureConfiguration> codec, int age) {
        super(codec);
        this.age = age;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("antarchy", "big_bush"));
        if (!(block instanceof BigBushBlock)) {
            return false;
        }

        List<Direction> facings = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        for (int i = facings.size() - 1; i > 0; i--) {
            Direction swap = facings.set(random.nextInt(i + 1), facings.get(i));
            facings.set(i, swap);
        }

        for (Direction facing : facings) {
            BlockState grown = block.defaultBlockState().setValue(BigBushBlock.AGE, age).setValue(BigBushBlock.FACING, facing);
            if (canPlace(level, origin, grown)) {
                BigBushBlock.placeBushBlocks(level, origin, grown, Block.UPDATE_ALL);
                return true;
            }
            if (age == 0) {
                // Facing is irrelevant at age 0 (single block); no need to try every rotation.
                break;
            }
        }
        return false;
    }

    private boolean canPlace(WorldGenLevel level, BlockPos origin, BlockState grown) {
        if (!BigBushBlock.hasFullSupport(grown, level, origin)) {
            return false;
        }
        List<BlockPos> positions = new ArrayList<>(BigBushBlock.partPositions(grown, origin));
        positions.add(origin);
        for (BlockPos pos : positions) {
            if (!level.getBlockState(pos).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }
}
