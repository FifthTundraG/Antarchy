package com.craisinlord.antarchy.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Invisible filler block for the grown {@link BigBushBlock} footprint. Breaking a
 * part destroys the whole bush (drops come from the origin's loot table). The one
 * part directly two above the origin of a stage-3 bush renders the top slice of
 * the oversized cross model
 */
public class BigBushPartBlock extends Block {
    public static final MapCodec<BigBushPartBlock> CODEC = Block.simpleCodec(BigBushPartBlock::new);

    public static final BooleanProperty CAP = BooleanProperty.create("cap");

    public BigBushPartBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CAP, false));
    }

    @Override
    public MapCodec<BigBushPartBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CAP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    /** Finds the origin bush this part belongs to, or null if orphaned. */
    @Nullable
    public static BlockPos findOrigin(LevelReader level, BlockPos partPos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -2; dy <= 0; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos candidate = partPos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(candidate);
                    if (state.getBlock() instanceof BigBushBlock
                            && BigBushBlock.partPositions(state, candidate).contains(partPos)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos origin = findOrigin(level, pos);
            if (origin != null) {
                BlockState originState = level.getBlockState(origin);
                if (!player.getAbilities().instabuild) {
                    Block.dropResources(originState, level, origin, null, player, player.getMainHandItem());
                }
                level.destroyBlock(origin, false);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos origin = findOrigin(level, pos);
            if (origin != null) {
                level.destroyBlock(origin, true);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        level.scheduleTick(pos, this, 1);
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos origin = findOrigin(level, pos);
        if (origin == null) {
            level.removeBlock(pos, false);
            return;
        }
        BlockState originState = level.getBlockState(origin);
        if (!originState.canSurvive(level, origin)) {
            level.destroyBlock(origin, true);
        }
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
        if (stack.is(Items.BONE_MEAL)) {
            BlockPos origin = findOrigin(level, pos);
            if (origin != null && BigBushBlock.grantBonemealBonus(level, origin, level.getBlockState(origin), player, stack)) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos origin = findOrigin(level, pos);
        if (origin != null) {
            return new ItemStack(level.getBlockState(origin).getBlock());
        }
        return ItemStack.EMPTY;
    }
}
