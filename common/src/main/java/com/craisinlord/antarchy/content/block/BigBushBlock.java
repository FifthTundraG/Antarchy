package com.craisinlord.antarchy.content.block;

import com.craisinlord.antarchy.Antarchy;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Multiblock bush grown with bonemeal. The origin block carries the
 * loot/age; the rest of the footprint is filled with invisible
 * {@link BigBushPartBlock}s.
 *
 * age 0: single block (1x1x1)
 * age 1: 2x2x2 footprint; FACING + FACING.getClockWise() pick the horizontal quadrant
 * age 2: 3x3x3 footprint centered on the origin column
 *
 * Grown stages require a sturdy support block under every column of their
 * footprint. Growing from 2x2 to 3x3 may relocate the origin to any of the four
 * occupied columns to find room.
 */
public class BigBushBlock extends BushBlock implements BonemealableBlock {
    public static final MapCodec<BigBushBlock> CODEC = Block.simpleCodec(BigBushBlock::new);

    public static final IntegerProperty AGE = BlockStateProperties.AGE_2;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SMALL_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public BigBushBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(AGE, 0)
                .setValue(FACING, Direction.EAST));
    }

    @Override
    public MapCodec<BigBushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AGE) == 0 ? SMALL_SHAPE : Shapes.block();
    }

    /** All part positions belonging to a bush with the given origin state/pos */
    public static List<BlockPos> partPositions(BlockState state, BlockPos origin) {
        List<BlockPos> positions = new ArrayList<>();
        int age = state.getValue(AGE);
        if (age == 1) {
            Direction forward = state.getValue(FACING);
            Direction side = forward.getClockWise();
            for (int dy = 0; dy <= 1; dy++) {
                for (BlockPos column : new BlockPos[]{
                        origin, origin.relative(forward), origin.relative(side), origin.relative(forward).relative(side)}) {
                    BlockPos pos = column.above(dy);
                    if (!pos.equals(origin)) {
                        positions.add(pos);
                    }
                }
            }
        } else if (age == 2) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        positions.add(origin.offset(dx, dy, dz));
                    }
                }
            }
        }
        return positions;
    }

    /** Ground columns of the footprint, each needs a sturdy block below it. */
    public static List<BlockPos> supportColumns(BlockState state, BlockPos origin) {
        List<BlockPos> columns = new ArrayList<>();
        int age = state.getValue(AGE);
        if (age == 1) {
            Direction forward = state.getValue(FACING);
            Direction side = forward.getClockWise();
            columns.add(origin);
            columns.add(origin.relative(forward));
            columns.add(origin.relative(side));
            columns.add(origin.relative(forward).relative(side));
        } else if (age == 2) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    columns.add(origin.offset(dx, 0, dz));
                }
            }
        } else {
            columns.add(origin);
        }
        return columns;
    }

    public static boolean hasFullSupport(BlockState state, LevelReader level, BlockPos origin) {
        for (BlockPos column : supportColumns(state, origin)) {
            BlockPos below = column.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(AGE) == 0) {
            return super.canSurvive(state, level, pos);
        }
        return hasFullSupport(state, level, pos);
    }

    private static Block partBlock() {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Antarchy.MODID, "big_bush_part"));
    }

    private record GrowthTarget(BlockPos origin, BlockState state) {}

    @Nullable
    private GrowthTarget findGrowthTarget(LevelReader level, BlockPos pos, BlockState state, RandomSource random) {
        int age = state.getValue(AGE);
        if (age == 0) {
            List<Direction> facings = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
            for (int i = facings.size() - 1; i > 0; i--) {
                Direction swap = facings.set(random.nextInt(i + 1), facings.get(i));
                facings.set(i, swap);
            }
            for (Direction facing : facings) {
                BlockState grown = state.setValue(AGE, 1).setValue(FACING, facing);
                if (canGrowTo(level, pos, grown, Set.of(pos))) {
                    return new GrowthTarget(pos, grown);
                }
            }
            return null;
        }
        if (age == 1) {
            Direction forward = state.getValue(FACING);
            Direction side = forward.getClockWise();
            Set<BlockPos> owned = new HashSet<>(partPositions(state, pos));
            owned.add(pos);
            BlockState grown = state.setValue(AGE, 2);
            // Any of the four occupied columns can become the new center of the 3x3.
            for (BlockPos candidate : new BlockPos[]{
                    pos, pos.relative(forward).relative(side), pos.relative(forward), pos.relative(side)}) {
                if (canGrowTo(level, candidate, grown, owned)) {
                    return new GrowthTarget(candidate, grown);
                }
            }
        }
        return null;
    }

    private boolean canGrowTo(LevelReader level, BlockPos newOrigin, BlockState grownState, Set<BlockPos> owned) {
        if (!hasFullSupport(grownState, level, newOrigin)) {
            return false;
        }
        List<BlockPos> positions = new ArrayList<>(partPositions(grownState, newOrigin));
        positions.add(newOrigin);
        for (BlockPos pos : positions) {
            if (owned.contains(pos)) {
                continue;
            }
            if (!level.getBlockState(pos).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    public static void placeBushBlocks(LevelAccessor level, BlockPos origin, BlockState grownState, int flags) {
        level.setBlock(origin, grownState, flags);
        Block part = partBlock();
        BlockPos capPos = grownState.getValue(AGE) == 2 ? origin.above(2) : null;
        for (BlockPos pos : partPositions(grownState, origin)) {
            level.setBlock(pos, part.defaultBlockState()
                    .setValue(BigBushPartBlock.CAP, pos.equals(capPos)), flags);
        }
    }

    private void placeStage(ServerLevel level, BlockPos origin, BlockState grownState) {
        placeBushBlocks(level, origin, grownState, 3);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            for (BlockPos partPos : partPositions(state, pos)) {
                if (level.getBlockState(partPos).getBlock() instanceof BigBushPartBlock) {
                    level.setBlock(partPos, Blocks.AIR.defaultBlockState(), 35);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < 2;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return findGrowthTarget(level, pos, state, random) != null;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        GrowthTarget target = findGrowthTarget(level, pos, state, random);
        if (target == null) {
            return;
        }
        if (!target.origin().equals(pos)) {
            level.removeBlock(pos, false);
        }
        placeStage(level, target.origin(), target.state());
    }
}
