package com.dexer.aquanaut.common.block;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractPipeBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final Direction[] SHAPE_DIRECTIONS = {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST,
            Direction.UP,
            Direction.DOWN
    };

    private static final Map<Direction, BooleanProperty> CONNECTIONS = new EnumMap<>(Direction.class);
    private static final VoxelShape[] SHAPES = new VoxelShape[1 << SHAPE_DIRECTIONS.length];
    private static final double MIN = 4.0D;
    private static final double MAX = 12.0D;
    private static final VoxelShape CENTER_SHAPE = Block.box(MIN, MIN, MIN, MAX, MAX, MAX);

    static {
        CONNECTIONS.put(Direction.NORTH, NORTH);
        CONNECTIONS.put(Direction.SOUTH, SOUTH);
        CONNECTIONS.put(Direction.EAST, EAST);
        CONNECTIONS.put(Direction.WEST, WEST);
        CONNECTIONS.put(Direction.UP, UP);
        CONNECTIONS.put(Direction.DOWN, DOWN);

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

    protected AbstractPipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return updateConnections(defaultBlockState()
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER), context.getLevel(),
                context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state.setValue(property(direction), canConnectTo(state, neighborState, direction));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, WATERLOGGED);
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
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    protected BlockState updateConnections(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState connected = state;
        for (Direction direction : SHAPE_DIRECTIONS) {
            BlockState neighborState = level.getBlockState(pos.relative(direction));
            connected = connected.setValue(property(direction), canConnectTo(connected, neighborState, direction));
        }
        return connected;
    }

    protected boolean canConnectTo(BlockState state, BlockState neighborState, Direction direction) {
        if (neighborState.getBlock() instanceof AbstractPipeBlock otherPipe) {
            return canConnectToPipe(state, otherPipe, neighborState, direction);
        }
        return canConnectToBlock(state, neighborState, direction);
    }

    protected boolean canConnectToPipe(BlockState state, AbstractPipeBlock otherPipe, BlockState neighborState,
            Direction direction) {
        return otherPipe.getClass() == getClass();
    }

    protected boolean canConnectToBlock(BlockState state, BlockState neighborState, Direction direction) {
        if (neighborState.getBlock() instanceof AirConnector connector) {
            return connector.connectsOnFace(neighborState, direction.getOpposite());
        }
        return false;
    }

    private static BooleanProperty property(Direction direction) {
        return CONNECTIONS.get(direction);
    }

    private static int shapeIndex(BlockState state) {
        int mask = 0;
        if (state.getValue(NORTH)) {
            mask |= 1;
        }
        if (state.getValue(SOUTH)) {
            mask |= 2;
        }
        if (state.getValue(EAST)) {
            mask |= 4;
        }
        if (state.getValue(WEST)) {
            mask |= 8;
        }
        if (state.getValue(UP)) {
            mask |= 16;
        }
        if (state.getValue(DOWN)) {
            mask |= 32;
        }
        return mask;
    }
}
