package com.dexer.aquanaut.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public abstract class AbstractAirConnectorBlock extends DirectionalBlock implements AirConnector {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    protected AbstractAirConnectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Direction getPipeConnectionFace(BlockState state) {
        return state.getValue(FACING).getOpposite();
    }

    public boolean connectsOnFace(BlockState state, Direction face) {
        return getPipeConnectionFace(state) == face;
    }

    public abstract int getFlowStrength(BlockState state);

    public abstract boolean isFlowSource();

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Rotation rotation = mirror.getRotation(state.getValue(FACING));
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    protected boolean isPipeAdjacent(BlockGetter level, BlockPos pos, BlockState state) {
        BlockPos pipePos = pos.relative(getPipeConnectionFace(state));
        return level.getBlockState(pipePos).getBlock() instanceof AbstractPipeBlock;
    }

    protected BlockState fixedFacing(BlockState state, Direction facing) {
        return state.setValue(FACING, facing);
    }
}
