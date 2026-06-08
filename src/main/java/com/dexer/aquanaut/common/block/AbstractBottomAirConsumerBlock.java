package com.dexer.aquanaut.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public abstract class AbstractBottomAirConsumerBlock extends Block implements AirConnector {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    protected AbstractBottomAirConsumerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    public final Direction getPipeConnectionFace(BlockState state) {
        return Direction.DOWN;
    }

    @Override
    public Direction getPipeFlowFace(BlockState state) {
        return Direction.UP;
    }

    @Override
    public final boolean isFlowSource() {
        return false;
    }

    @Override
    public final int getFlowStrength(BlockState state) {
        return -Math.abs(getDemandStrength(state));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        return context.getLevel() == null ? state : AirFlowActivation.setActive(state,
                AirFlowActivation.isActive(context.getLevel(), context.getClickedPos(), state));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
            boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide) {
            AirFlowActivation.refresh(level, pos);
        }
    }

    protected abstract int getDemandStrength(BlockState state);

    protected abstract MapCodec<? extends Block> codec();
}
