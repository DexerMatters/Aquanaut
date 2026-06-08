package com.dexer.aquanaut.common.block.entity;

import com.dexer.aquanaut.common.block.AirFlowActivation;
import com.dexer.aquanaut.common.block.AirConnector;
import com.dexer.aquanaut.common.pipe.AirFlowSolver;
import com.dexer.aquanaut.common.pipe.GasPipeNetworkSolver;
import com.dexer.aquanaut.core.BlockEntityRegistry;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class GasPipeBlockEntity extends AbstractPipeBlockEntity {
    private static final Direction[] DIRECTIONS = Direction.values();

    public GasPipeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GAS_PIPE.get(), pos, state);
    }

    public void tickServer() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        long gameTime = level.getGameTime();
        prepareFlowSnapshot(gameTime);

        EnumSet<AirFlowSolver.Endpoint> pipeFaces = EnumSet.noneOf(AirFlowSolver.Endpoint.class);
        EnumMap<AirFlowSolver.Endpoint, Integer> incomingPipeFlow = new EnumMap<>(AirFlowSolver.Endpoint.class);
        EnumMap<AirFlowSolver.Endpoint, Integer> sourceInputs = new EnumMap<>(AirFlowSolver.Endpoint.class);
        EnumMap<AirFlowSolver.Endpoint, Integer> sinkDemands = new EnumMap<>(AirFlowSolver.Endpoint.class);

        for (Direction direction : DIRECTIONS) {
            if (!isConnected(getBlockState(), direction)) {
                continue;
            }

            BlockPos neighborPos = worldPosition.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            AirFlowSolver.Endpoint endpoint = endpoint(direction);

            if (neighborState.getBlock() instanceof com.dexer.aquanaut.common.block.AbstractPipeBlock) {
                pipeFaces.add(endpoint);
                BlockEntity neighborEntity = level.getBlockEntity(neighborPos);
                if (neighborEntity instanceof AbstractPipeBlockEntity neighborPipe) {
                    neighborPipe.prepareFlowSnapshot(gameTime);
                    int amount = Math.max(0, neighborPipe.getSampledFaceFlow(direction.getOpposite()));
                    if (amount > 0) {
                        incomingPipeFlow.put(endpoint, amount);
                    }
                }
                continue;
            }

            if (neighborState.getBlock() instanceof AirConnector connector
                    && connector.connectsOnFace(neighborState, direction.getOpposite())) {
                int amount = Math.abs(connector.getFlowStrength(neighborState));
                if (amount <= 0) {
                    continue;
                }
                if (connector.isFlowSource()) {
                    sourceInputs.put(endpoint, amount);
                } else {
                    sinkDemands.put(endpoint, amount);
                }
            }
        }

        GasPipeNetworkSolver.FlowResult result = GasPipeNetworkSolver.solve(new GasPipeNetworkSolver.NodeSnapshot(
                pipeFaces,
                incomingPipeFlow,
                sourceInputs,
                sinkDemands));

        EnumMap<Direction, Integer> flows = new EnumMap<>(Direction.class);
        for (Map.Entry<AirFlowSolver.Endpoint, Integer> entry : result.faceFlows().entrySet()) {
            flows.put(direction(entry.getKey()), entry.getValue());
        }
        setFaceFlows(flows);
        AirFlowActivation.refreshAroundPipe(level, worldPosition);
    }

    private boolean isConnected(BlockState state, Direction direction) {
        return switch (direction) {
            case NORTH -> state.getValue(com.dexer.aquanaut.common.block.AbstractPipeBlock.NORTH);
            case SOUTH -> state.getValue(com.dexer.aquanaut.common.block.AbstractPipeBlock.SOUTH);
            case EAST -> state.getValue(com.dexer.aquanaut.common.block.AbstractPipeBlock.EAST);
            case WEST -> state.getValue(com.dexer.aquanaut.common.block.AbstractPipeBlock.WEST);
            case UP -> state.getValue(com.dexer.aquanaut.common.block.AbstractPipeBlock.UP);
            case DOWN -> state.getValue(com.dexer.aquanaut.common.block.AbstractPipeBlock.DOWN);
        };
    }

    private static AirFlowSolver.Endpoint endpoint(Direction direction) {
        return switch (direction) {
            case NORTH -> AirFlowSolver.Endpoint.NORTH;
            case SOUTH -> AirFlowSolver.Endpoint.SOUTH;
            case EAST -> AirFlowSolver.Endpoint.EAST;
            case WEST -> AirFlowSolver.Endpoint.WEST;
            case UP -> AirFlowSolver.Endpoint.UP;
            case DOWN -> AirFlowSolver.Endpoint.DOWN;
        };
    }

    private static Direction direction(AirFlowSolver.Endpoint endpoint) {
        return switch (endpoint) {
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case EAST -> Direction.EAST;
            case WEST -> Direction.WEST;
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
            case CENTER -> throw new IllegalArgumentException("Center endpoint does not map to a pipe face");
        };
    }
}
