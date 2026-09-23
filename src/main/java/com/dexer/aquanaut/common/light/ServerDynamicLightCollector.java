package com.dexer.aquanaut.common.light;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Mutable per-frame collection owned by the server light engine. */
public final class ServerDynamicLightCollector {
    private final ResourceLocation provider;
    private final Map<DynamicLightId, ServerDynamicLightRequest> requests = new LinkedHashMap<>();

    ServerDynamicLightCollector(ResourceLocation provider) {
        this.provider = provider;
    }

    public void submit(DynamicLightOwner owner, int channel, BlockPos position, int level,
            LightTransition transition) {
        submit(new DynamicLightId(provider, owner, channel), position, level, transition);
    }

    public void submit(DynamicLightId id, BlockPos position, int level, LightTransition transition) {
        if (!id.provider().equals(provider)) {
            throw new IllegalArgumentException("request provider does not match collector provider");
        }
        if (level > 0) {
            requests.put(id, new ServerDynamicLightRequest(id, position, level, transition));
        }
    }

    Collection<ServerDynamicLightRequest> requests() {
        return requests.values();
    }
}
