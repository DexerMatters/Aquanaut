package com.dexer.aquanaut.common.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SeaweedStemBlock extends AbstractPipeBlock {
    public static final MapCodec<SeaweedStemBlock> CODEC = simpleCodec(SeaweedStemBlock::new);

    private static final double MIN = 5.0D;
    private static final double MAX = 11.0D;
    private static final VoxelShape CENTER_SHAPE = Block.box(MIN, MIN, MIN, MAX, MAX, MAX);
    private static final VoxelShape[] SHAPES = new VoxelShape[1 << 6];

    static {
        for (int mask = 0; mask < SHAPES.length; mask++) {
            VoxelShape shape = CENTER_SHAPE;
            if ((mask & 1) != 0) {
                shape = Shapes.or(shape, Block.box(MIN, MIN, 0.0D, MAX, MAX, MIN));
            }
            if ((mask & 2) != 0) {
                shape = Shapes.or(shape, Block.box(MIN, MIN, MAX, MAX, MAX, 16.0D));
            }
            if ((mask & 4) != 0) {
                shape = Shapes.or(shape, Block.box(MAX, MIN, MIN, 16.0D, MAX, MAX));
            }
            if ((mask & 8) != 0) {
                shape = Shapes.or(shape, Block.box(0.0D, MIN, MIN, MIN, MAX, MAX));
            }
            if ((mask & 16) != 0) {
                shape = Shapes.or(shape, Block.box(MIN, MAX, MIN, MAX, 16.0D, MAX));
            }
            if ((mask & 32) != 0) {
                shape = Shapes.or(shape, Block.box(MIN, 0.0D, MIN, MAX, MIN, MAX));
            }
            SHAPES[mask] = shape;
        }
    }

    public SeaweedStemBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.NORTH, false)
                .setValue(BlockStateProperties.EAST, false)
                .setValue(BlockStateProperties.SOUTH, false)
                .setValue(BlockStateProperties.WEST, false)
                .setValue(BlockStateProperties.UP, false)
                .setValue(BlockStateProperties.DOWN, false)
                .setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    protected MapCodec<SeaweedStemBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        if (!fluidState.is(Fluids.WATER)) {
            return null;
        }
        return updateConnections(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true),
                context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        return updated.canSurvive(level, currentPos) ? updated : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(BlockStateProperties.WATERLOGGED);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            destroyColumnAbove(level, pos.above());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean canConnectToPipe(BlockState state, AbstractPipeBlock otherPipe, BlockState neighborState,
            Direction direction) {
        return SeaweedStemRules.connectsToSameStem(otherPipe.getClass());
    }

    @Override
    protected boolean canConnectToBlock(BlockState state, BlockState neighborState, Direction direction) {
        return SeaweedStemRules.connectsToSeaweedFamily(neighborState.getBlock().getClass());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[shapeIndex(state)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    private static int shapeIndex(BlockState state) {
        int mask = 0;
        if (state.getValue(BlockStateProperties.NORTH)) {
            mask |= 1;
        }
        if (state.getValue(BlockStateProperties.SOUTH)) {
            mask |= 2;
        }
        if (state.getValue(BlockStateProperties.EAST)) {
            mask |= 4;
        }
        if (state.getValue(BlockStateProperties.WEST)) {
            mask |= 8;
        }
        if (state.getValue(BlockStateProperties.UP)) {
            mask |= 16;
        }
        if (state.getValue(BlockStateProperties.DOWN)) {
            mask |= 32;
        }
        return mask;
    }

    private void destroyColumnAbove(Level level, BlockPos startPos) {
        BlockPos cursor = startPos;
        while (level.getBlockState(cursor).is(this)) {
            level.destroyBlock(cursor, true);
            cursor = cursor.above();
        }
    }
}
