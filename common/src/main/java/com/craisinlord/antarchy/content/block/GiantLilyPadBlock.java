package com.craisinlord.antarchy.content.block;

import com.craisinlord.antarchy.content.AntarchyObjects;
import com.craisinlord.antarchy.content.AntarchyTags;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class GiantLilyPadBlock extends Block implements BonemealableBlock {
    public static final MapCodec<GiantLilyPadBlock> CODEC = Block.simpleCodec(GiantLilyPadBlock::new);
    public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);
    public static final EnumProperty<TilePosition> TILE_POSITION = EnumProperty.create("tile_position", TilePosition.class);
    public static final EnumProperty<PadRotation> ROTATION = EnumProperty.create("rotation", PadRotation.class);

    private static final VoxelShape SHAPE_BOX = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 0.5D, 16.0D);
    private static final int SEARCH_RADIUS = 2;
    private static final ThreadLocal<Integer> RESTRUCTURING_DEPTH = ThreadLocal.withInitial(() -> 0);

    public GiantLilyPadBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(SHAPE, Shape.SINGLE)
                .setValue(TILE_POSITION, TilePosition.SINGLE)
                .setValue(ROTATION, PadRotation.DEG_0));
    }

    @Override
    public MapCodec<GiantLilyPadBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, TILE_POSITION, ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return canSurvive(defaultBlockState(), context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BOX;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isSupported(level, pos);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        if (isThreeByThreeCenter(state)) {
            return level.isEmptyBlock(pos.above());
        }

        return state.getValue(SHAPE) != Shape.THREE_BY_THREE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (isThreeByThreeCenter(state)) {
            growLotus(level, pos);
            return;
        }

        if (state.getValue(SHAPE) == Shape.THREE_BY_THREE) {
            return;
        }

        spreadNearby(level, random, pos);
    }

    private static boolean isThreeByThreeCenter(BlockState state) {
        return state.getValue(SHAPE) == Shape.THREE_BY_THREE && state.getValue(TILE_POSITION) == TilePosition.CENTER;
    }

    private static void growLotus(ServerLevel level, BlockPos padPos) {
        BlockPos abovePos = padPos.above();
        if (!level.isEmptyBlock(abovePos)) {
            return;
        }

        BlockState lotusState = AntarchyObjects.LOTUS.get().defaultBlockState();
        if (lotusState.canSurvive(level, abovePos)) {
            level.setBlock(abovePos, lotusState, Block.UPDATE_ALL);
        }
    }

    private static void spreadNearby(ServerLevel level, RandomSource random, BlockPos origin) {
        for (int attempt = 0; attempt < 6; attempt++) {
            int dx = random.nextInt(5) - 2;
            int dz = random.nextInt(5) - 2;
            if (dx == 0 && dz == 0) {
                continue;
            }

            BlockPos candidate = origin.offset(dx, 0, dz);
            if (!level.isEmptyBlock(candidate) || !isSupported(level, candidate)) {
                continue;
            }

            level.setBlock(candidate, AntarchyObjects.GIANT_LILY_PAD.get().defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            if (level instanceof ServerLevel serverLevel) {
                recalculateNearbyPads(serverLevel, pos);
            }
            return Blocks.AIR.defaultBlockState();
        }

        if (level instanceof ServerLevel serverLevel) {
            recalculateNearbyPads(serverLevel, pos);
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(state.getBlock()) && level instanceof ServerLevel serverLevel) {
            recalculateNearbyPads(serverLevel, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            recalculateNearbyPads(serverLevel, pos);
        }
    }

    private static boolean isSupported(LevelReader level, BlockPos pos) {
        return level.getFluidState(pos).is(AntarchyTags.Fluids.GIANT_LILY_PAD_SUPPORTING_FLUIDS)
                || level.getFluidState(pos.below()).is(AntarchyTags.Fluids.GIANT_LILY_PAD_SUPPORTING_FLUIDS);
    }

    private static void recalculateNearbyPads(ServerLevel level, BlockPos center) {
        if (isRestructuring()) {
            return;
        }

        withRestructuringGuard(() -> {
            int minX = center.getX() - SEARCH_RADIUS;
            int maxX = center.getX() + SEARCH_RADIUS;
            int minZ = center.getZ() - SEARCH_RADIUS;
            int maxZ = center.getZ() + SEARCH_RADIUS;
            int y = center.getY();

            List<ShapeCandidate> candidates = new ArrayList<>();
            collectCandidates(level, y, minX, maxX, minZ, maxZ, candidates);

            candidates.sort(Comparator
                    .comparingInt(ShapeCandidate::area).reversed()
                    .thenComparingInt(candidate -> candidate.anchor().getX())
                    .thenComparingInt(candidate -> candidate.anchor().getZ())
                    .thenComparingInt(candidate -> candidate.shape().ordinal()));

            List<BlockPos> assigned = new ArrayList<>();
            for (ShapeCandidate candidate : candidates) {
                boolean blocked = false;
                for (BlockPos pos : candidate.positions()) {
                    if (assigned.contains(pos)) {
                        blocked = true;
                        break;
                    }
                }
                if (blocked) {
                    continue;
                }

                PadRotation rotation = rotationFor(level, candidate.anchor());
                for (TileAssignment tile : candidate.tiles()) {
                    BlockPos pos = tile.pos();
                    BlockState newState = level.getBlockState(pos);
                    if (!newState.is(candidate.block())) {
                        continue;
                    }
                    BlockState shapedState = candidate.block().defaultBlockState()
                            .setValue(SHAPE, candidate.shape())
                            .setValue(TILE_POSITION, tile.tilePosition())
                            .setValue(ROTATION, rotation);
                    if (!newState.equals(shapedState)) {
                        level.setBlock(pos, shapedState, Block.UPDATE_CLIENTS);
                    }
                    assigned.add(pos);
                }
            }
        });
    }

    private static void collectCandidates(ServerLevel level, int y, int minX, int maxX, int minZ, int maxZ, List<ShapeCandidate> out) {
        addRectCandidates(level, y, minX, maxX, minZ, maxZ, Shape.THREE_BY_THREE, GridLayouts.THREE_BY_THREE, out);
        addRectCandidates(level, y, minX, maxX, minZ, maxZ, Shape.TWO_BY_TWO, GridLayouts.TWO_BY_TWO, out);
        addRectCandidates(level, y, minX, maxX, minZ, maxZ, Shape.TWO_BY_ONE, GridLayouts.TWO_BY_ONE_HORIZONTAL, out);
        addRectCandidates(level, y, minX, maxX, minZ, maxZ, Shape.TWO_BY_ONE, GridLayouts.TWO_BY_ONE_VERTICAL, out);
        addRectCandidates(level, y, minX, maxX, minZ, maxZ, Shape.SINGLE, GridLayouts.SINGLE, out);
    }

    private static void addRectCandidates(ServerLevel level, int y, int minX, int maxX, int minZ, int maxZ, Shape shape, GridLayout layout, List<ShapeCandidate> out) {
        int width = layout.width();
        int height = layout.height();
        for (int anchorX = minX; anchorX <= maxX - width + 1; anchorX++) {
            for (int anchorZ = minZ; anchorZ <= maxZ - height + 1; anchorZ++) {
                BlockPos anchor = new BlockPos(anchorX, y, anchorZ);
                ShapeCandidate candidate = createCandidate(level, anchor, shape, layout);
                if (candidate != null) {
                    out.add(candidate);
                }
            }
        }
    }

    @Nullable
    private static ShapeCandidate createCandidate(ServerLevel level, BlockPos anchor, Shape shape, GridLayout layout) {
        BlockState anchorState = level.getBlockState(anchor);
        if (!(anchorState.getBlock() instanceof GiantLilyPadBlock giantLilyPadBlock)) {
            return null;
        }

        List<TileAssignment> tiles = new ArrayList<>(layout.width() * layout.height());
        for (int row = 0; row < layout.height(); row++) {
            for (int column = 0; column < layout.width(); column++) {
                BlockPos pos = anchor.offset(column, 0, row);
                if (!level.getBlockState(pos).is(giantLilyPadBlock)) {
                    return null;
                }
                TilePosition tilePosition = layout.tiles()[row][column];
                tiles.add(new TileAssignment(pos, tilePosition));
            }
        }

        return new ShapeCandidate(giantLilyPadBlock, shape, anchor, tiles);
    }

    private static boolean isRestructuring() {
        return RESTRUCTURING_DEPTH.get() > 0;
    }

    private static void withRestructuringGuard(Runnable action) {
        RESTRUCTURING_DEPTH.set(RESTRUCTURING_DEPTH.get() + 1);
        try {
            action.run();
        } finally {
            RESTRUCTURING_DEPTH.set(Math.max(0, RESTRUCTURING_DEPTH.get() - 1));
        }
    }

    private static PadRotation rotationFor(ServerLevel level, BlockPos anchor) {
        long seed = level.getSeed();
        long mix = seed
                ^ anchor.asLong()
                ^ Long.rotateLeft(seed, 17)
                ^ Long.rotateLeft(anchor.asLong(), 31);
        int index = (int) Math.floorMod(mix, 4L);
        return PadRotation.values()[index];
    }

    private record ShapeCandidate(GiantLilyPadBlock block, Shape shape, BlockPos anchor, List<TileAssignment> tiles) {
        int area() {
            return this.tiles.size();
        }

        List<BlockPos> positions() {
            return this.tiles.stream().map(TileAssignment::pos).toList();
        }
    }

    private record TileAssignment(BlockPos pos, TilePosition tilePosition) {
    }

    private record GridLayout(TilePosition[][] tiles) {
        int width() {
            return this.tiles[0].length;
        }

        int height() {
            return this.tiles.length;
        }
    }

    private static final class GridLayouts {
        private static final GridLayout SINGLE = new GridLayout(new TilePosition[][]{
                {TilePosition.SINGLE}
        });

        private static final GridLayout TWO_BY_ONE_HORIZONTAL = new GridLayout(new TilePosition[][]{
                {TilePosition.MIDDLE_LEFT, TilePosition.MIDDLE_RIGHT}
        });

        private static final GridLayout TWO_BY_ONE_VERTICAL = new GridLayout(new TilePosition[][]{
                {TilePosition.TOP_MIDDLE},
                {TilePosition.BOTTOM_MIDDLE}
        });

        private static final GridLayout TWO_BY_TWO = new GridLayout(new TilePosition[][]{
                {TilePosition.TOP_LEFT, TilePosition.TOP_RIGHT},
                {TilePosition.BOTTOM_LEFT, TilePosition.BOTTOM_RIGHT}
        });

        private static final GridLayout THREE_BY_THREE = new GridLayout(new TilePosition[][]{
                {TilePosition.TOP_LEFT, TilePosition.TOP_MIDDLE, TilePosition.TOP_RIGHT},
                {TilePosition.MIDDLE_LEFT, TilePosition.CENTER, TilePosition.MIDDLE_RIGHT},
                {TilePosition.BOTTOM_LEFT, TilePosition.BOTTOM_MIDDLE, TilePosition.BOTTOM_RIGHT}
        });

        private GridLayouts() {
        }
    }

    public enum Shape implements StringRepresentable {
        SINGLE("single"),
        TWO_BY_ONE("two_by_one"),
        TWO_BY_TWO("two_by_two"),
        THREE_BY_THREE("three_by_three");

        private final String name;

        Shape(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public enum TilePosition implements StringRepresentable {
        SINGLE("single"),
        TOP_LEFT("top_left"),
        TOP_MIDDLE("top_middle"),
        TOP_RIGHT("top_right"),
        MIDDLE_LEFT("middle_left"),
        CENTER("center"),
        MIDDLE_RIGHT("middle_right"),
        BOTTOM_LEFT("bottom_left"),
        BOTTOM_MIDDLE("bottom_middle"),
        BOTTOM_RIGHT("bottom_right");

        private final String name;

        TilePosition(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public enum PadRotation implements StringRepresentable {
        DEG_0("0"),
        DEG_90("90"),
        DEG_180("180"),
        DEG_270("270");

        private final String name;

        PadRotation(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
