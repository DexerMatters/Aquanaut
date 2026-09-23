package com.dexer.aquanaut.client.light;

import java.util.Objects;

import com.dexer.aquanaut.common.light.DynamicLightId;
import net.minecraft.world.phys.Vec3;

/** One continuous client-side point-light sample. */
public record ClientDynamicLightRequest(DynamicLightId id, Vec3 position, int level, double radius) {
    public ClientDynamicLightRequest {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(position, "position");
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
            throw new IllegalArgumentException("position must be finite");
        }
        if (level < 0 || level > 15) {
            throw new IllegalArgumentException("level must be between 0 and 15");
        }
        if (!(radius > 0.0D) || !Double.isFinite(radius)) {
            throw new IllegalArgumentException("radius must be finite and positive");
        }
    }
}
