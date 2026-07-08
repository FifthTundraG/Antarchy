package com.craisinlord.antarchy.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeepingVinesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings({"rawtypes", "unchecked"})
public class OuranwoodVineBlock extends WeepingVinesBlock {
    public static final MapCodec<OuranwoodVineBlock> CODEC = simpleCodec(OuranwoodVineBlock::new);
    private static Block bodyBlock;

    public OuranwoodVineBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<WeepingVinesBlock> codec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected Block getBodyBlock() {
        return bodyBlock;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    public static void bindBodyBlock(Block block) {
        bodyBlock = block;
    }
}
