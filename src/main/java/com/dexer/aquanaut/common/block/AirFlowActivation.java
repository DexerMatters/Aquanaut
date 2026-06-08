package com.dexer.aquanaut.common.block;

import com.dexer.aquanaut.common.block.entity.AbstractPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class AirFlowActivation {
    private static final Direction[] DIRECTIONS = Direction.values();

    private AirFlowActivation() {
    }

    public static void refreshAroundPipe(Level level, BlockPos pipePos) {
        if (level.isClientSide) {
            return;
        }
        for (Direction direction : DIRECTIONS) {
            refresh(level, pipePos.relative(direction));
        }
    }

    public static void refresh(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!isReactive(block)) {
            return;
        }

        boolean active = isActive(level, pos, state);
        if (isActiveState(state) == active) {
            return;
        }
        BlockState updated = setActive(state, active);
        if (updated != state) {
            level.setBlock(pos, updated, 3);
        }
    }

    public static boolean isActive(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof AirConnector connector) {
            Direction connectionFace = connector.getPipeConnectionFace(state);
            Direction flowFace = connector.getPipeFlowFace(state);
            BlockPos pipePos = pos.relative(connectionFace);
            return connector.isFlowSource()
                    ? pipeHasNegativeFlow(level, pipePos, flowFace)
                    : pipeHasPositiveFlow(level, pipePos, flowFace);
        }
        return false;
    }

    public static BlockState setActive(BlockState state, boolean active) {
        Block block = state.getBlock();
        if (block instanceof OmnidirectionalMachineBlock) {
            return state.setValue(OmnidirectionalMachineBlock.ACTIVE, active);
        }
        if (block instanceof BubbleMachineBlock) {
            return state.setValue(BubbleMachineBlock.ACTIVE, active);
        }
        if (block instanceof AirPumpBlock) {
            return state.setValue(AirPumpBlock.ACTIVE, active);
        }
        if (block instanceof AbstractBottomAirConsumerBlock) {
            return state.setValue(AbstractBottomAirConsumerBlock.ACTIVE, active);
        }
        return state;
    }

    private static boolean isReactive(Block block) {
        return block instanceof OmnidirectionalMachineBlock
                || block instanceof BubbleMachineBlock
                || block instanceof AirPumpBlock
                || block instanceof AbstractBottomAirConsumerBlock;
    }

    private static boolean pipeHasPositiveFlow(Level level, BlockPos pipePos, Direction face) {
        BlockState pipeState = level.getBlockState(pipePos);
        if (!(pipeState.getBlock() instanceof AbstractPipeBlock)) {
            return false;
        }
        if (!(level.getBlockEntity(pipePos) instanceof AbstractPipeBlockEntity pipeEntity)) {
            return false;
        }
        return pipeEntity.getFaceFlow(face) > 0;
    }

    private static boolean pipeHasNegativeFlow(Level level, BlockPos pipePos, Direction face) {
        BlockState pipeState = level.getBlockState(pipePos);
        if (!(pipeState.getBlock() instanceof AbstractPipeBlock)) {
            return false;
        }
        if (!(level.getBlockEntity(pipePos) instanceof AbstractPipeBlockEntity pipeEntity)) {
            return false;
        }
        return pipeEntity.getFaceFlow(face) < 0;
    }

    private static boolean isActiveState(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof OmnidirectionalMachineBlock) {
            return state.getValue(OmnidirectionalMachineBlock.ACTIVE);
        }
        if (block instanceof BubbleMachineBlock) {
            return state.getValue(BubbleMachineBlock.ACTIVE);
        }
        if (block instanceof AirPumpBlock) {
            return state.getValue(AirPumpBlock.ACTIVE);
        }
        if (block instanceof AbstractBottomAirConsumerBlock) {
            return state.getValue(AbstractBottomAirConsumerBlock.ACTIVE);
        }
        return false;
    }
}
