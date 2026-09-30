package com.dexer.aquanaut.common.worldgen.layers;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Spatial mix of 1..N biomes on a single depth layer.
 * Two entries share one noise field with a soft threshold (organic coral/jelly borders).
 * N entries use per-entry noise channels; winner is argmax of weight × smoothed affinity.
 *
 * <p>Sampling happens in <em>continuous</em> quart space through a two-octave domain
 * warp, so district borders meander at two scales (peninsulas and bays instead of drawn
 * contours) and any consumer — biome palette, terrain field, feature placement — sees
 * the very same warped field. Block-resolution consumers interpolate the quart lattice
 * through {@link com.dexer.aquanaut.common.worldgen.blend.DistrictWeightField} instead of
 * flooring to {@code block >> 2}, which removes the 4-block stair-step of the raw
 * per-quart sampling.</p>
 */
public record BiomeMix(List<MixEntry> entries, double horizontalBlendQuarts) {
    /** Seed of the boundary domain warp; shared by every mix so borders meander coherently. */
    private static final long WARP_SEED = 0xB1E4D5EEDL;
    /** Wavelength of the broad warp bend, in quarts. */
    private static final double WARP_BROAD_CELL = 24.0D;
    /** Wavelength of the fine warp wiggle, in quarts. */
    private static final double WARP_FINE_CELL = 7.0D;
    /** Full border displacement at {@code horizontal_blend_quarts >= 4}, in quarts. */
    private static final double WARP_FULL_AMPLITUDE = 3.2D;

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
        return weightsAtContinuous(quartX, quartZ, new double[entries.size()]);
    }

    /** Zero-allocation variant writing into {@code out} (length ≥ entry count). */
    public double[] weightsAt(int quartX, int quartZ, double[] out) {
        return weightsAtContinuous(quartX, quartZ, out);
    }

    /**
     * Continuous-coordinate weights: the same field as {@link #weightsAt(int, int)} but
     * sampled between lattice points, for warping and block-resolution interpolation.
     */
    public double[] weightsAtContinuous(double quartX, double quartZ, double[] out) {
        int n = entries.size();
        if (n > out.length) {
            throw new IllegalArgumentException("weight output array too small");
        }
        if (n == 1) {
            out[0] = 1.0D;
            return out;
        }

        double warpX = quartX + warp(quartX, quartZ, WARP_SEED);
        double warpZ = quartZ + warp(quartX, quartZ, WARP_SEED ^ 0x9E3779B97F4A7C15L);

        if (n == 2) {
            MixEntry a = entries.get(0);
            MixEntry b = entries.get(1);
            int cell = Math.max(1, a.noiseScaleQuarts());
            double noise = SoftMixNoise.valueNoise(
                    warpX + a.noiseOffsetX(),
                    warpZ + a.noiseOffsetZ(),
                    cell,
                    a.noiseSalt());
            double blend = SoftMixNoise.clamp01(horizontalBlendQuarts / (double) cell) * 0.8D;
            double[] pair = SoftMixNoise.softBinaryWeights(noise, blend);
            double wa = Math.max(1e-6, a.weight());
            double wb = Math.max(1e-6, b.weight());
            double sum = wa + wb;
            out[0] = pair[0] * (wa / sum) * 2.0D;
            out[1] = pair[1] * (wb / sum) * 2.0D;
            return normalize(out, 2, out[0] + out[1]);
        }

        double total = 0.0D;
        for (int i = 0; i < n; i++) {
            MixEntry entry = entries.get(i);
            int cell = Math.max(1, entry.noiseScaleQuarts());
            double noise = SoftMixNoise.valueNoise(
                    warpX + entry.noiseOffsetX(),
                    warpZ + entry.noiseOffsetZ(),
                    cell,
                    entry.noiseSalt());
            double affinity = SoftMixNoise.smoothstep((noise + 1.0D) * 0.5D);
            out[i] = Math.max(1e-6, entry.weight()) * Math.max(1e-6, affinity);
            total += out[i];
        }
        return normalize(out, n, total);
    }

    /**
     * Organic border meander: a two-octave domain warp of the sample position, so
     * transitions read as wandering coastlines instead of clean drawn contours. The
     * displacement follows the layer's declared {@code horizontal_blend_quarts}
     * (full bend at 4 quarts of blend, no bend at 0).
     */
    private double warp(double quartX, double quartZ, long seed) {
        double amplitude = SoftMixNoise.clamp01(horizontalBlendQuarts / 4.0D) * WARP_FULL_AMPLITUDE;
        if (amplitude <= 0.0D) {
            return 0.0D;
        }
        double broad = SoftMixNoise.valueNoise(quartX, quartZ, WARP_BROAD_CELL, seed);
        double fine = SoftMixNoise.valueNoise(quartX, quartZ, WARP_FINE_CELL, seed ^ 0x51EDL);
        return amplitude * (0.70D * broad + 0.30D * fine);
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

    private static double[] normalize(double[] weights, int count, double total) {
        if (total <= 0.0D) {
            double even = 1.0D / count;
            for (int i = 0; i < count; i++) {
                weights[i] = even;
            }
            return weights;
        }
        for (int i = 0; i < count; i++) {
            weights[i] /= total;
        }
        return weights;
    }
}
