package com.dexer.aquanaut.common.block;

import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractAirConsumerBlock extends AbstractAirConnectorBlock {
    protected AbstractAirConsumerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public final boolean isFlowSource() {
        return false;
    }

    @Override
    public final int getFlowStrength(BlockState state) {
        return -Math.abs(getDemandStrength(state));
    }

    protected abstract int getDemandStrength(BlockState state);
}
