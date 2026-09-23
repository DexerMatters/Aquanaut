package com.dexer.aquanaut.client.light;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.dexer.aquanaut.common.light.DynamicLightId;
import com.dexer.aquanaut.common.light.DynamicLightOwner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Mutable frame collection owned by the client light engine. */
public final class ClientDynamicLightCollector {
    private final ResourceLocation provider;
    private final Map<DynamicLightId, ClientDynamicLightRequest> requests = new LinkedHashMap<>();

    ClientDynamicLightCollector(ResourceLocation provider) {
        this.provider = provider;
    }

    public void submit(DynamicLightOwner owner, int channel, Vec3 position, int level, double radius) {
        submit(new DynamicLightId(provider, owner, channel), position, level, radius);
    }

    public void submit(DynamicLightId id, Vec3 position, int level, double radius) {
        if (!id.provider().equals(provider)) {
            throw new IllegalArgumentException("request provider does not match collector provider");
        }
        if (level > 0) {
            requests.put(id, new ClientDynamicLightRequest(id, position, level,
                    Math.min(ClientDynamicLightManager.DEFAULT_RADIUS, radius)));
        }
    }

    Collection<ClientDynamicLightRequest> requests() {
        return requests.values();
    }
}
