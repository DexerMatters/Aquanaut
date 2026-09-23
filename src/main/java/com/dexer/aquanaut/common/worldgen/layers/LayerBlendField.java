package com.dexer.aquanaut.common.worldgen.layers;

import net.minecraft.resources.ResourceLocation;

/**
 * Continuous influence weights for one chunk: region edge × horizontal mix × vertical band.
 */
public final class LayerBlendField {
    private final OceanLayerStack stack;
    private final double[] edgeAtBlock;
    private final int baseQuartX;
    private final int baseQuartZ;

    public LayerBlendField(OceanLayerStack stack, double[] edgeAtBlock, int baseQuartX, int baseQuartZ) {
        this.stack = stack;
        this.edgeAtBlock = edgeAtBlock;
        this.baseQuartX = baseQuartX;
        this.baseQuartZ = baseQuartZ;
    }

    public OceanLayerStack stack() {
        return stack;
    }

    public double edgeAtLocalBlock(int localX, int localZ) {
        return edgeAtBlock[(localX & 15) * 16 + (localZ & 15)];
    }

    public double weight(int layerIndex, int entryIndex, int worldQuartX, int worldQuartZ, int blockY,
                         int localX, int localZ) {
        OceanLayer layer = stack.layers().get(layerIndex);
        double vertical = layer.band().weightAtBlockY(blockY);
        if (vertical <= 0.0D) {
            return 0.0D;
        }
        double horizontal = layer.mix().weightsAt(worldQuartX, worldQuartZ)[entryIndex];
        MixEntry entry = layer.mix().entries().get(entryIndex);
        if (entry.inheritSurface()) {
            return vertical * horizontal;
        }
        return vertical * horizontal * edgeAtLocalBlock(localX, localZ);
    }

    /**
     * Dominant custom (non-inherit) biome at a quart column and block Y, or null if none.
     */
    public ResourceLocation dominantCustomBiome(int worldQuartX, int worldQuartZ, int blockY,
                                                int localX, int localZ) {
        double best = 0.0D;
        ResourceLocation bestBiome = null;
        for (OceanLayer layer : stack.layers()) {
            if (!layer.rewritesBiome()) {
                continue;
            }
            double vertical = layer.band().weightAtBlockY(blockY) * edgeAtLocalBlock(localX, localZ);
            if (vertical <= 0.0D) {
                continue;
            }
            double[] mix = layer.mix().weightsAt(worldQuartX, worldQuartZ);
            for (int entryIndex = 0; entryIndex < mix.length; entryIndex++) {
                MixEntry entry = layer.mix().entries().get(entryIndex);
                if (entry.inheritSurface()) {
                    continue;
                }
                double w = vertical * mix[entryIndex];
                if (w > best) {
                    best = w;
                    bestBiome = entry.biome();
                }
            }
        }
        return bestBiome;
    }

    public double[] horizontalMixAt(int worldQuartX, int worldQuartZ, int blockY) {
        return stack.dominantLayerAtBlockY(blockY).mix().weightsAt(worldQuartX, worldQuartZ);
    }

    public int baseQuartX() {
        return baseQuartX;
    }

    public int baseQuartZ() {
        return baseQuartZ;
    }
}
