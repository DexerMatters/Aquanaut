package com.dexer.aquanaut.common.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Anything the gas pipe network can push flow into or out of.
 *
 * <p>
 * The pipe asks a neighbour for the face it connects on and for a signed flow strength, and it flips
 * the neighbour's {@link #ACTIVE} state when the network reaches it. Keeping the property and the
 * two accessors here (rather than in each block class) means the activation logic never has to know
 * which blocks exist.
 */
public interface AirConnector {

    /** Set by the network on connectors that show whether they are currently being fed. */
    BooleanProperty ACTIVE = BooleanProperty.create("active");

    Direction getPipeConnectionFace(BlockState state);

    default Direction getPipeFlowFace(BlockState state) {
        return getPipeConnectionFace(state).getOpposite();
    }

    default boolean connectsOnFace(BlockState state, Direction face) {
        return getPipeConnectionFace(state) == face;
    }

    /** Positive when the connector feeds the network, negative when it draws from it. */
    int getFlowStrength(BlockState state);

    boolean isFlowSource();

    /** Whether this connector shows the network's {@link #ACTIVE} state at all. */
    default boolean isActiveIndicator() {
        return false;
    }

    default boolean isActive(BlockState state) {
        return this.isActiveIndicator() && state.getValue(ACTIVE);
    }

    default BlockState withActive(BlockState state, boolean active) {
        return this.isActiveIndicator() ? state.setValue(ACTIVE, active) : state;
    }
}
