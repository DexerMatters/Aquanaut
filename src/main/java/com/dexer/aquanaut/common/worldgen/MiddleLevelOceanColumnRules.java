package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;

public final class MiddleLevelOceanColumnRules {
    private static final int FULL_QUART_CELL_COLUMN_COUNT = 16;

    private MiddleLevelOceanColumnRules() {
    }

    public static boolean supportsQuartCell(ResourceLocation surfaceBiomeLocation, int openWaterColumns) {
        return MiddleLevelOceanPlacement.isVanillaDeepOcean(surfaceBiomeLocation)
                && openWaterColumns >= FULL_QUART_CELL_COLUMN_COUNT;
    }

    public static TargetBiome targetBiome(ResourceLocation surfaceBiomeLocation, int openWaterColumns, int quartY) {
        if (!supportsQuartCell(surfaceBiomeLocation, openWaterColumns)) {
            return TargetBiome.NONE;
        }

        if (CoralForestPlacement.isCoralForestQuartY(quartY)) {
            return TargetBiome.CORAL_FOREST;
        }

        if (MiddleLevelOceanPlacement.isMiddleLayerQuartY(quartY)) {
            return TargetBiome.MIDDLE_LEVEL_OCEAN;
        }

        return TargetBiome.NONE;
    }

    public enum TargetBiome {
        NONE,
        CORAL_FOREST,
        MIDDLE_LEVEL_OCEAN
    }
}
