package com.dexer.aquanaut.client.light;

import net.minecraft.client.multiplayer.ClientLevel;

/** Collects a provider's visual light samples for one rendered frame. */
@FunctionalInterface
public interface ClientDynamicLightProvider {
    void collect(ClientLevel level, float partialTick, ClientDynamicLightCollector collector);
}
