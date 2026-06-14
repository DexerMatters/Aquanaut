package com.dexer.aquanaut.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DroopingSeaweedBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<DroopingSeaweedBlock> CODEC = simpleCodec(DroopingSeaweedBlock::new);
    public static final EnumProperty<SeaweedPart> PART = EnumProperty.create("part", SeaweedPart.class);
    private static final VoxelShape TOP_SHAPE = Shapes.or(
            Block.box(2.0D, 0.0D, 5.0D, 14.0D, 16.0D, 11.0D),
            Block.box(5.0D, 0.0D, 2.0D, 11.0D, 16.0D, 14.0D));
    private static final VoxelShape BODY_SHAPE = Shapes.or(
            Block.box(3.0D, 0.0D, 5.0D, 13.0D, 16.0D, 11.0D),
            Block.box(5.0D, 0.0D, 3.0D, 11.0D, 16.0D, 13.0D));
    private static final VoxelShape TAIL_SHAPE = Shapes.or(
            Block.box(4.0D, 0.0D, 5.0D, 12.0D, 16.0D, 11.0D),
            Block.box(5.0D, 0.0D, 4.0D, 11.0D, 16.0D, 12.0D));

    public DroopingSeaweedBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(PART, SeaweedPart.TOP)
                .setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    protected MapCodec<DroopingSeaweedBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) && state.getValue(PART) != SeaweedPart.BODY;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluidState = level.getFluidState(pos);
        if (fluidState.getType() != Fluids.WATER) {
            return null;
        }
        SeaweedPart part = SeaweedPart.TOP;
        BlockState aboveState = level.getBlockState(pos.above());
        if (aboveState.is(this)) {
            part = SeaweedPart.TAIL;
        }
        return defaultBlockState()
                .setValue(PART, part)
                .setValue(BlockStateProperties.WATERLOGGED, true);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction == Direction.DOWN
                && neighborState.is(this)
                && state.getValue(PART) == SeaweedPart.TAIL) {
            return state.setValue(PART, SeaweedPart.BODY);
        }
        return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() != this) {
            return;
        }

        if (!canSurvive(current, level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }

        if (state.getValue(PART) == SeaweedPart.BODY || random.nextInt(12) != 0) {
            return;
        }

        SeaweedPart part = state.getValue(PART);
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        boolean belowIsWater = level.getFluidState(below).is(Fluids.WATER);
        boolean belowReplaceable = belowState.canBeReplaced() || belowState.is(Blocks.WATER);

        if (!belowIsWater || !belowReplaceable) {
            return;
        }

        if (part == SeaweedPart.TOP) {
            level.setBlock(below, defaultBlockState()
                    .setValue(PART, SeaweedPart.TAIL)
                    .setValue(BlockStateProperties.WATERLOGGED, true), Block.UPDATE_CLIENTS);
        } else if (part == SeaweedPart.TAIL) {
            level.setBlock(pos, state.setValue(PART, SeaweedPart.BODY), Block.UPDATE_CLIENTS);
            level.setBlock(below, defaultBlockState()
                    .setValue(PART, SeaweedPart.TAIL)
                    .setValue(BlockStateProperties.WATERLOGGED, true), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        SeaweedPart part = state.getValue(PART);
        if (part == SeaweedPart.TOP) {
            return state.getValue(BlockStateProperties.WATERLOGGED);
        }
        BlockState above = level.getBlockState(pos.above());
        return above.getBlock() == this;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, BlockStateProperties.WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(PART)) {
            case TOP -> TOP_SHAPE;
            case BODY -> BODY_SHAPE;
            case TAIL -> TAIL_SHAPE;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return Shapes.empty();
    }

    public enum SeaweedPart implements StringRepresentable {
        TOP("top"),
        BODY("body"),
        TAIL("tail");

        private final String serializedName;

        SeaweedPart(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
