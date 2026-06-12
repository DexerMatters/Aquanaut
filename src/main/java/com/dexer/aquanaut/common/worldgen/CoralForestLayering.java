package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;

public final class CoralForestLayering {
    private CoralForestLayering() {
    }

    public static boolean shouldReplaceBiome(ResourceLocation upperBiomeLocation, int quartY) {
        return upperBiomeLocation != null
                && CoralForestPlacement.isVanillaOcean(upperBiomeLocation)
                && CoralForestPlacement.isCoralForestQuartY(quartY);
    }
}
