package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;

public final class MiddleLevelOceanLayering {
    private MiddleLevelOceanLayering() {
    }

    public static boolean shouldReplaceBiome(ResourceLocation upperBiomeLocation, int quartY) {
        return upperBiomeLocation != null
                && MiddleLevelOceanPlacement.isVanillaOcean(upperBiomeLocation)
                && MiddleLevelOceanPlacement.isMiddleLayerQuartY(quartY);
    }
}
