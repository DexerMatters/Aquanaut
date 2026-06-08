package com.dexer.aquanaut.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class BubbleMachineBlock extends AbstractAirConsumerBlock {
    public static final MapCodec<BubbleMachineBlock> CODEC = simpleCodec(BubbleMachineBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public BubbleMachineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<BubbleMachineBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return context.getLevel() == null ? state : AirFlowActivation.setActive(state,
                AirFlowActivation.isActive(context.getLevel(), context.getClickedPos(), state));
    }

    @Override
    protected int getDemandStrength(BlockState state) {
        return 8;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
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
}
