package com.craisinlord.antarchy.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.WeepingVinesBlock;
import net.minecraft.world.level.block.WeepingVinesPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings({"rawtypes", "unchecked"})
public class OuranwoodVinePlantBlock extends WeepingVinesPlantBlock {
    public static final MapCodec<OuranwoodVinePlantBlock> CODEC = simpleCodec(OuranwoodVinePlantBlock::new);
    private static GrowingPlantHeadBlock headBlock;

    public OuranwoodVinePlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<WeepingVinesPlantBlock> codec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected GrowingPlantHeadBlock getHeadBlock() {
        return headBlock;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    public static void bindHeadBlock(WeepingVinesBlock block) {
        headBlock = block;
    }
}
