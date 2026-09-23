package com.dexer.aquanaut.common.light;

import net.minecraft.server.level.ServerLevel;

/** Collects a provider's active block-light requests for one server level and tick. */
@FunctionalInterface
public interface ServerDynamicLightProvider {
    void collect(ServerLevel level, ServerDynamicLightCollector collector);
}
