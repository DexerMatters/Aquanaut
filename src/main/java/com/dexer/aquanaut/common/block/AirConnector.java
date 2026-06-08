package com.dexer.aquanaut.common.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public interface AirConnector {
    Direction getPipeConnectionFace(BlockState state);

    default Direction getPipeFlowFace(BlockState state) {
        return getPipeConnectionFace(state).getOpposite();
    }

    default boolean connectsOnFace(BlockState state, Direction face) {
        return getPipeConnectionFace(state) == face;
    }

    int getFlowStrength(BlockState state);

    boolean isFlowSource();
}
