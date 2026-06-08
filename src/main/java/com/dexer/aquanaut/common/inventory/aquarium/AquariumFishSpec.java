package com.dexer.aquanaut.common.inventory.aquarium;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public record AquariumFishSpec(
        ResourceLocation id,
        EntityType<?> entityType,
        ResourceLocation modelLocation,
        float modelHeight,
        int gridWidth,
        int gridHeight) {

    public AquariumFishSpec {
        if (modelHeight <= 0.0F) {
            throw new IllegalArgumentException("Fish model height must be positive");
        }
        if (gridWidth < 1 || gridHeight < 1) {
            throw new IllegalArgumentException("Fish footprint must be at least 1x1");
        }
    }

    public AquariumPlacementMath.Placement placement(int anchorIndex) {
        return new AquariumPlacementMath.Placement(anchorIndex, gridWidth, gridHeight);
    }

    public String commandId() {
        return id.toString();
    }
}
