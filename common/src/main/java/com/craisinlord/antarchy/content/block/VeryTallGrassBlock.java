package com.craisinlord.antarchy.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class VeryTallGrassBlock extends BushBlock implements BonemealableBlock {
    public static final MapCodec<VeryTallGrassBlock> CODEC = Block.simpleCodec(VeryTallGrassBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public VeryTallGrassBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, Part.LOWER));
    }

    @Override
    public MapCodec<VeryTallGrassBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return switch (state.getValue(PART)) {
            case LOWER -> super.canSurvive(state, level, pos);
            case MIDDLE -> level.getBlockState(pos.below()).is(this)
                    && level.getBlockState(pos.below()).getValue(PART) == Part.LOWER
                    && level.getBlockState(pos.above()).is(this)
                    && level.getBlockState(pos.above()).getValue(PART) == Part.UPPER;
            case UPPER -> level.getBlockState(pos.below()).is(this)
                    && level.getBlockState(pos.below()).getValue(PART) == Part.MIDDLE;
        };
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        Part part = state.getValue(PART);
        if (part == Part.LOWER && direction == Direction.UP && (!neighborState.is(this) || neighborState.getValue(PART) != Part.MIDDLE)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (part == Part.MIDDLE) {
            if (direction == Direction.DOWN && (!neighborState.is(this) || neighborState.getValue(PART) != Part.LOWER)) {
                return Blocks.AIR.defaultBlockState();
            }
            if (direction == Direction.UP && (!neighborState.is(this) || neighborState.getValue(PART) != Part.UPPER)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        if (part == Part.UPPER && direction == Direction.DOWN && (!neighborState.is(this) || neighborState.getValue(PART) != Part.MIDDLE)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state.canSurvive(level, pos) ? super.updateShape(state, direction, neighborState, level, pos, neighborPos) : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (player.getItemInHand(hand).is(Items.BONE_MEAL)) {
            popResource(level, pos, new ItemStack(state.getBlock().asItem(), 1));
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (!level.getBlockState(pos.above()).canBeReplaced(context) || !level.getBlockState(pos.above(2)).canBeReplaced(context)) {
            return null;
        }
        return super.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        placeAt(level, pos, Block.UPDATE_ALL);
    }

    public static void placeAt(LevelAccessor level, BlockPos pos, int flags) {
        level.setBlock(pos, defaultState(Part.LOWER), flags);
        level.setBlock(pos.above(), defaultState(Part.MIDDLE), flags);
        level.setBlock(pos.above(2), defaultState(Part.UPPER), flags);
    }

    private static BlockState defaultState(Part part) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("antarchy", "very_tall_grass"))
                .defaultBlockState()
                .setValue(PART, part);
    }

    public enum Part implements StringRepresentable {
        LOWER("lower"),
        MIDDLE("middle"),
        UPPER("upper");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
