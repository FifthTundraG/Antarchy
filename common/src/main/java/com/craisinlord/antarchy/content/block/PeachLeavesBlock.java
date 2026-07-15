package com.craisinlord.antarchy.content.block;

import com.craisinlord.antarchy.Antarchy;
import com.craisinlord.antarchy.content.AntarchyObjects;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class PeachLeavesBlock extends LeavesBlock implements BonemealableBlock {
    public static final MapCodec<PeachLeavesBlock> CODEC = Block.simpleCodec(PeachLeavesBlock::new);
    private static final ResourceLocation HANGING_PEACH_ID = ResourceLocation.fromNamespaceAndPath(Antarchy.MODID, "hanging_peach");

    public PeachLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(DISTANCE, DECAY_DISTANCE)
                .setValue(PERSISTENT, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public MapCodec<PeachLeavesBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(PERSISTENT, true)
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(18) != 0) {
            return;
        }

        level.addParticle(
                AntarchyObjects.PEACH_LEAVES_PARTICLE.get(),
                pos.getX() + random.nextDouble(),
                pos.getY() + random.nextDouble(),
                pos.getZ() + random.nextDouble(),
                0.0D,
                -0.02D,
                0.0D
        );
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos.below()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos.below()).isAir()) {
            return;
        }

        level.setBlock(pos.below(), createHangingPeachState(), Block.UPDATE_ALL);
    }

    public static BlockState createHangingPeachState() {
        return BuiltInRegistries.BLOCK.getOptional(HANGING_PEACH_ID)
                .map(Block::defaultBlockState)
                .orElseThrow(() -> new IllegalStateException("Missing hanging peach block: " + HANGING_PEACH_ID));
    }
}
