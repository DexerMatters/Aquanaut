package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.common.worldgen.layers.OceanLayerStack;
import com.dexer.aquanaut.common.worldgen.layers.OceanLayerStacks;
import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;
import net.minecraft.resources.ResourceLocation;

/**
 * Thin facade over the active {@link OceanLayerStack}. Prefer the layers package for new code.
 */
public final class MiddleLevelOceanColumnRules {
    private MiddleLevelOceanColumnRules() {
    }

    public static boolean supportsQuartCell(ResourceLocation surfaceBiomeLocation, int openWaterColumns) {
        return OceanLayerStacks.active().supportsQuartCell(surfaceBiomeLocation, openWaterColumns);
    }

    public static TargetBiome targetBiome(ResourceLocation surfaceBiomeLocation,
                                          int openWaterColumns,
                                          int quartX,
                                          int quartY,
                                          int quartZ) {
        OceanLayerStack stack = OceanLayerStacks.active();
        if (!stack.supportsQuartCell(surfaceBiomeLocation, openWaterColumns)) {
            return TargetBiome.NONE;
        }

        int blockY = (quartY << 2) + 2;
        ResourceLocation biome = stack.dominantLayerAtBlockY(blockY).mix().dominantBiomeAt(quartX, quartZ);
        if (biome == null) {
            return TargetBiome.NONE;
        }
        if (biome.equals(CoralForestPlacement.location())) {
            return TargetBiome.CORAL_FOREST;
        }
        if (biome.equals(JellyJunglePlacement.location())) {
            return TargetBiome.JELLY_JUNGLE;
        }
        if (biome.equals(MiddleLevelOceanPlacement.location())) {
            return TargetBiome.MIDDLE_LEVEL_OCEAN;
        }
        if (biome.equals(BrineMirrorGorgePlacement.location())) {
            return TargetBiome.BRINE_MIRROR_GORGE;
        }
        if (biome.equals(BrimstoneCalderaPlacement.location())) {
            return TargetBiome.BRIMSTONE_CALDERA;
        }
        if (biome.equals(CrystalNestPlacement.location())) {
            return TargetBiome.CRYSTAL_NEST;
        }
        if (biome.equals(DeepSeaPlacement.location())) {
            return TargetBiome.DEEP_SEA;
        }
        return TargetBiome.NONE;
    }

    /**
     * Soft horizontal mix weight for jelly jungle on the reef band (1 = full jelly).
     */
    public static double jellyWeight(int quartX, int quartZ) {
        OceanLayerStack stack = OceanLayerStacks.active();
        for (var layer : stack.layers()) {
            if (layer.mix().entries().size() >= 2
                    && layer.mix().entries().get(0).biome().equals(CoralForestPlacement.location())) {
                double[] weights = layer.mix().weightsAt(quartX + 11, quartZ - 7);
                return weights[1];
            }
        }
        return SoftMixNoise.softThreshold(
                SoftMixNoise.valueNoise(quartX + 11, quartZ - 7, 32, 2L),
                0.05D);
    }

    public enum TargetBiome {
        NONE,
        CORAL_FOREST,
        JELLY_JUNGLE,
        MIDDLE_LEVEL_OCEAN,
        BRINE_MIRROR_GORGE,
        BRIMSTONE_CALDERA,
        CRYSTAL_NEST,
        DEEP_SEA
    }
}
