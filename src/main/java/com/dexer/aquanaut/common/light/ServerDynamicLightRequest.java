package com.dexer.aquanaut.common.light;

import java.util.Objects;

import net.minecraft.core.BlockPos;

/** One server-side point-light contribution for the current collection frame. */
public record ServerDynamicLightRequest(
        DynamicLightId id,
        BlockPos position,
        int level,
        LightTransition transition) {

    public ServerDynamicLightRequest {
        Objects.requireNonNull(id, "id");
        position = Objects.requireNonNull(position, "position").immutable();
        Objects.requireNonNull(transition, "transition");
        if (level < 0 || level > 15) {
            throw new IllegalArgumentException("level must be between 0 and 15");
        }
    }
}
