package com.dexer.aquanaut.common.worldgen.layers;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Full vertical story under qualifying deep-ocean columns.
 */
public record OceanLayerStack(ResourceLocation id,
                              List<OceanLayer> layers,
                              double verticalBlendQuarts,
                              double regionEdgeFadeBlocks,
                              int minOpenWaterColumns,
                              List<ResourceLocation> parentBiomes) {

    public OceanLayerStack {
        if (layers == null || layers.isEmpty()) {
            throw new IllegalArgumentException("OceanLayerStack requires at least one layer");
        }
        layers = List.copyOf(layers);
        parentBiomes = List.copyOf(parentBiomes);
    }

    public boolean isParentBiome(ResourceLocation biomeLocation) {
        return biomeLocation != null && parentBiomes.contains(biomeLocation);
    }

    /**
     * This stack as it applies to a world whose floor sits at {@code minBuildHeight}.
     *
     * <p>
     * Extending the world's Y range changes exactly one thing: the deepest band — the abyss, the
     * last layer — grows downward until it is as tall as the band above it. Everything else keeps
     * the position and height it was authored with: the surface ocean, the reef ceiling, the
     * middle sea and its chamber all stay put. The area under the abyss band is deliberately left
     * unclaimed, so future abyssal content has somewhere to go.
     * </p>
     *
     * <p>
     * At the reference floor the world floor clamps the band back to its authored extent, and the
     * stack comes back unchanged.
     * </p>
     */
    public OceanLayerStack deepenedFor(int minBuildHeight) {
        if (layers.size() < 2) {
            return this;
        }
        int deepest = layers.size() - 1;
        DepthBand band = layers.get(deepest).band();
        if (minBuildHeight >= band.minY()) {
            return this;
        }
        DepthBand above = layers.get(deepest - 1).band();
        int abyssHeight = above.maxY() - above.minY() + 1;
        int min = Math.max(minBuildHeight, band.maxY() - abyssHeight + 1);
        if (min >= band.minY()) {
            return this;
        }
        List<OceanLayer> extended = new ArrayList<>(layers);
        OceanLayer abyss = layers.get(deepest);
        extended.set(deepest, new OceanLayer(abyss.id(),
                new DepthBand(min, band.maxY(), band.blendDown(), band.blendUp()),
                abyss.mix(), abyss.terrain(), abyss.carve()));
        return new OceanLayerStack(id, extended, verticalBlendQuarts, regionEdgeFadeBlocks,
                minOpenWaterColumns, parentBiomes);
    }

    public int minRewriteBlockY() {
        int min = Integer.MAX_VALUE;
        for (OceanLayer layer : layers) {
            if (layer.rewritesBiome()) {
                min = Math.min(min, layer.band().minY());
            }
        }
        return min == Integer.MAX_VALUE ? Integer.MIN_VALUE / 4 : min;
    }

    public int maxRewriteBlockY() {
        int max = Integer.MIN_VALUE;
        for (OceanLayer layer : layers) {
            if (layer.rewritesBiome()) {
                max = Math.max(max, layer.band().maxY());
            }
        }
        return max == Integer.MIN_VALUE ? Integer.MIN_VALUE : max;
    }

    public int minRewriteQuartY() {
        return QuartY.fromBlock(minRewriteBlockY());
    }

    public int maxRewriteQuartY() {
        return QuartY.fromBlock(maxRewriteBlockY());
    }

    public double[] layerWeightsAtBlockY(int blockY) {
        double[] weights = new double[layers.size()];
        double total = 0.0D;
        for (int i = 0; i < layers.size(); i++) {
            double w = layers.get(i).band().weightAtBlockY(blockY);
            weights[i] = w;
            total += w;
        }
        if (total <= 0.0D) {
            int best = 0;
            double bestDist = Double.POSITIVE_INFINITY;
            for (int i = 0; i < layers.size(); i++) {
                DepthBand band = layers.get(i).band();
                double center = (band.minY() + (double) band.maxY()) * 0.5D;
                double dist = Math.abs(blockY - center);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = i;
                }
            }
            weights[best] = 1.0D;
            return weights;
        }
        for (int i = 0; i < weights.length; i++) {
            weights[i] /= total;
        }
        return weights;
    }

    public OceanLayer dominantLayerAtBlockY(int blockY) {
        double[] weights = layerWeightsAtBlockY(blockY);
        int best = 0;
        for (int i = 1; i < weights.length; i++) {
            if (weights[i] > weights[best]) {
                best = i;
            }
        }
        return layers.get(best);
    }

    public boolean supportsQuartCell(ResourceLocation surfaceBiomeLocation, int openWaterColumns) {
        return isParentBiome(surfaceBiomeLocation) && openWaterColumns >= minOpenWaterColumns;
    }
}
