package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;

public final class MiddleLevelOceanColumnRules {
    private static final int FULL_QUART_CELL_COLUMN_COUNT = 16;
    private static final int TRANSITION_PATCH_SIZE_QUARTS = 32;

    private MiddleLevelOceanColumnRules() {
    }

    public static boolean supportsQuartCell(ResourceLocation surfaceBiomeLocation, int openWaterColumns) {
        return MiddleLevelOceanPlacement.isVanillaDeepOcean(surfaceBiomeLocation)
                && openWaterColumns >= FULL_QUART_CELL_COLUMN_COUNT;
    }

    public static TargetBiome targetBiome(ResourceLocation surfaceBiomeLocation,
                                          int openWaterColumns,
                                          int quartX,
                                          int quartY,
                                          int quartZ) {
        if (!supportsQuartCell(surfaceBiomeLocation, openWaterColumns)) {
            return TargetBiome.NONE;
        }

        if (CoralForestPlacement.isCoralForestQuartY(quartY)) {
            return transitionBandBiome(quartX, quartZ);
        }

        if (MiddleLevelOceanPlacement.isMiddleLayerQuartY(quartY)) {
            return TargetBiome.MIDDLE_LEVEL_OCEAN;
        }

        return TargetBiome.NONE;
    }

    private static TargetBiome transitionBandBiome(int quartX, int quartZ) {
        double noise = transitionNoise(quartX + 11, quartZ - 7);
        return noise >= 0.0D ? TargetBiome.JELLY_JUNGLE : TargetBiome.CORAL_FOREST;
    }

    private static double transitionNoise(int quartX, int quartZ) {
        int cellX = Math.floorDiv(quartX, TRANSITION_PATCH_SIZE_QUARTS);
        int cellZ = Math.floorDiv(quartZ, TRANSITION_PATCH_SIZE_QUARTS);
        double fracX = Math.floorMod(quartX, TRANSITION_PATCH_SIZE_QUARTS)
                / (double) TRANSITION_PATCH_SIZE_QUARTS;
        double fracZ = Math.floorMod(quartZ, TRANSITION_PATCH_SIZE_QUARTS)
                / (double) TRANSITION_PATCH_SIZE_QUARTS;
        double sx = smoothstep(fracX);
        double sz = smoothstep(fracZ);

        double n00 = cornerNoise(cellX, cellZ);
        double n10 = cornerNoise(cellX + 1, cellZ);
        double n01 = cornerNoise(cellX, cellZ + 1);
        double n11 = cornerNoise(cellX + 1, cellZ + 1);
        double nx0 = lerp(sx, n00, n10);
        double nx1 = lerp(sx, n01, n11);
        return lerp(sz, nx0, nx1);
    }

    private static double cornerNoise(int cellX, int cellZ) {
        long hash = mix(cellX, cellZ, 2);
        return (Math.floorMod(hash, 2001L) / 1000.0D) - 1.0D;
    }

    private static double smoothstep(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    private static long mix(int cellX, int cellZ, int salt) {
        long value = 0x9E3779B97F4A7C15L;
        value ^= (long) cellX * 341873128712L;
        value ^= (long) cellZ * 132897987541L;
        value ^= (long) salt * 0x94D049BB133111EBL;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    public enum TargetBiome {
        NONE,
        CORAL_FOREST,
        JELLY_JUNGLE,
        MIDDLE_LEVEL_OCEAN
    }
}
