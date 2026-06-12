package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;

public final class MiddleLevelOceanTransitionSupport {
    private MiddleLevelOceanTransitionSupport() {
    }

    public static boolean supportsCurrentChunkCell(ResourceLocation biomeLocation, int openWaterColumns) {
        return MiddleLevelOceanColumnRules.supportsQuartCell(biomeLocation, openWaterColumns);
    }

    public static boolean supportsHaloCell(ResourceLocation biomeLocation) {
        return biomeLocation != null && MiddleLevelOceanPlacement.isVanillaDeepOcean(biomeLocation);
    }
}
