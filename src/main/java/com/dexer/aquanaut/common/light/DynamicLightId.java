package com.dexer.aquanaut.common.light;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/** Stable identity for one provider-owned light channel. */
public record DynamicLightId(ResourceLocation provider, DynamicLightOwner owner, int channel) {
    public DynamicLightId {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(owner, "owner");
        if (channel < 0) {
            throw new IllegalArgumentException("channel must be non-negative");
        }
    }
}
