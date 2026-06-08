package com.dexer.aquanaut.common.block;

import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractAirSourceBlock extends AbstractAirConnectorBlock {
    protected AbstractAirSourceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public final boolean isFlowSource() {
        return true;
    }

    @Override
    public final int getFlowStrength(BlockState state) {
        return Math.abs(getOutputStrength(state));
    }

    protected abstract int getOutputStrength(BlockState state);
}
