package com.dexer.aquanaut.common.worldgen.layers;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Spatial mix of 1..N biomes on a single depth layer.
 * Two entries share one noise field with a soft threshold (organic coral/jelly borders).
 * N entries use per-entry noise channels; winner is argmax of weight × smoothed affinity.
 */
public record BiomeMix(List<MixEntry> entries, double horizontalBlendQuarts) {

    public BiomeMix {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("BiomeMix requires at least one entry");
        }
        entries = List.copyOf(entries);
    }

    public static BiomeMix single(ResourceLocation biome) {
        return new BiomeMix(List.of(MixEntry.of(biome)), 0.0D);
    }

    public static BiomeMix inheritSurface() {
        return new BiomeMix(List.of(MixEntry.surfaceInherited()), 0.0D);
    }

    /**
     * Continuous weights aligned with {@link #entries()}, summing to 1.
     */
    public double[] weightsAt(int quartX, int quartZ) {
        int n = entries.size();
        double[] weights = new double[n];
        if (n == 1) {
            weights[0] = 1.0D;
            return weights;
        }

        if (n == 2) {
            MixEntry a = entries.get(0);
            MixEntry b = entries.get(1);
            int cell = Math.max(1, a.noiseScaleQuarts());
            double noise = SoftMixNoise.valueNoise(
                    quartX + a.noiseOffsetX(),
                    quartZ + a.noiseOffsetZ(),
                    cell,
                    a.noiseSalt());
            noise += bend(quartX + a.noiseOffsetX(), quartZ + a.noiseOffsetZ(),
                    cell, a.noiseSalt());
            double blend = SoftMixNoise.clamp01(horizontalBlendQuarts / (double) cell) * 0.8D;
            double[] pair = SoftMixNoise.softBinaryWeights(noise, blend);
            double wa = Math.max(1e-6, a.weight());
            double wb = Math.max(1e-6, b.weight());
            double sum = wa + wb;
            weights[0] = pair[0] * (wa / sum) * 2.0D;
            weights[1] = pair[1] * (wb / sum) * 2.0D;
            return normalize(weights);
        }

        double total = 0.0D;
        for (int i = 0; i < n; i++) {
            MixEntry entry = entries.get(i);
            int cell = Math.max(1, entry.noiseScaleQuarts());
            double noise = SoftMixNoise.valueNoise(
                    quartX + entry.noiseOffsetX(),
                    quartZ + entry.noiseOffsetZ(),
                    cell,
                    entry.noiseSalt());
            noise += bend(quartX + entry.noiseOffsetX(), quartZ + entry.noiseOffsetZ(),
                    cell, entry.noiseSalt());
            double affinity = SoftMixNoise.smoothstep((noise + 1.0D) * 0.5D);
            weights[i] = Math.max(1e-6, entry.weight()) * Math.max(1e-6, affinity);
            total += weights[i];
        }
        return normalize(weights, total);
    }

    /**
     * Organic border ripple: two octaves of the value-noise family bend district borders
     * into peninsulas and bays at two scales, so transitions read as meandering coastlines
     * instead of clean drawn contours. The amplitude follows the layer's declared
     * {@code horizontal_blend_quarts} (full bend at 4 quarts of blend).
     */
    private double bend(int quartX, int quartZ, int cell, long seed) {
        double amplitude = SoftMixNoise.clamp01(horizontalBlendQuarts / 4.0D) * 0.18D;
        if (amplitude <= 0.0D) {
            return 0.0D;
        }
        double broad = SoftMixNoise.valueNoise(quartX, quartZ, Math.max(2, cell / 2), seed ^ 0x9E37L);
        double fine = SoftMixNoise.valueNoise(quartX, quartZ, Math.max(2, cell / 4), seed ^ 0x51L);
        return (broad * 0.6D + fine * 0.4D) * amplitude;
    }

    public MixEntry dominantAt(int quartX, int quartZ) {
        double[] weights = weightsAt(quartX, quartZ);
        int best = 0;
        for (int i = 1; i < weights.length; i++) {
            if (weights[i] > weights[best]) {
                best = i;
            }
        }
        return entries.get(best);
    }

    public ResourceLocation dominantBiomeAt(int quartX, int quartZ) {
        return dominantAt(quartX, quartZ).biome();
    }

    private static double[] normalize(double[] weights) {
        double total = 0.0D;
        for (double weight : weights) {
            total += weight;
        }
        return normalize(weights, total);
    }

    private static double[] normalize(double[] weights, double total) {
        if (total <= 0.0D) {
            double even = 1.0D / weights.length;
            for (int i = 0; i < weights.length; i++) {
                weights[i] = even;
            }
            return weights;
        }
        for (int i = 0; i < weights.length; i++) {
            weights[i] /= total;
        }
        return weights;
    }
}
