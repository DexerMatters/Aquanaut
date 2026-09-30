package com.dexer.aquanaut.common.worldgen.blend;

import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;

/**
 * Shared pure maths of the terrain blend strategies: smooth windows, softened maxima,
 * fractal noise sums and deterministic dither draws.
 *
 * <p>Every function here is a pure, translation-invariant function of its inputs so any
 * field built from them is seamless across chunk borders and unit-testable without a
 * Minecraft runtime.</p>
 */
public final class BlendMath {
    private BlendMath() {
    }

    public static double clamp01(double value) {
        return SoftMixNoise.clamp01(value);
    }

    /** Hermite smoothstep on [0,1] — C1, zero derivative at both ends. */
    public static double smoothstep(double value) {
        return SoftMixNoise.smoothstep(value);
    }

    /** Quintic fade t⁶·6 − t⁵·15 + t⁴·10 — C2, zero first and second derivative at both ends. */
    public static double quintic(double value) {
        double t = clamp01(value);
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    /**
     * C1 bump window over u ∈ [-1, 1]: 1 at the centre, and both value and slope reach
     * exactly zero at |u| = 1. Anything gated through this window tapers out instead of
     * ending on a step — the replacement for hard {@code if (|field| < band)} carves.
     */
    public static double taper(double u) {
        double a = Math.abs(u);
        if (a >= 1.0D) {
            return 0.0D;
        }
        return quintic(1.0D - a);
    }

    /**
     * Smooth weighted maximum (stabilised log-sum-exp). C∞ in every input; approaches the
     * hard max as {@code p → ∞}, from below and within {@code ln(W/w_max)/p} of it
     * ({@code W} = total weight, {@code w_max} = weight of the largest value), so
     * overlapping relief features merge through rounded saddles instead of kinked
     * max-seams without ever overtopping the tallest contribution.
     */
    public static double softMax(double[] values, double[] weights, double p) {
        double max = Double.NEGATIVE_INFINITY;
        double totalWeight = 0.0D;
        for (int i = 0; i < values.length; i++) {
            double w = weights == null || i >= weights.length ? 1.0D : Math.max(0.0D, weights[i]);
            if (w <= 0.0D) {
                continue;
            }
            totalWeight += w;
            max = Math.max(max, values[i]);
        }
        if (totalWeight <= 0.0D) {
            // No positive weights: no meaningful maximum — callers decide their fallback.
            return Double.NaN;
        }
        if (p <= 0.0D) {
            return max;
        }
        double sum = 0.0D;
        for (int i = 0; i < values.length; i++) {
            double w = weights == null || i >= weights.length ? 1.0D : Math.max(0.0D, weights[i]);
            if (w <= 0.0D) {
                continue;
            }
            sum += w * Math.exp(p * (values[i] - max));
        }
        return max + Math.log(sum / totalWeight) / p;
    }

    /**
     * Plain fractal sum of the shared value-noise family, normalised back to [-1, 1].
     */
    public static double fbm(double x, double z, double cellSize, int octaves, double gain,
                             double lacunarity, long seed) {
        double amplitude = 1.0D;
        double total = 0.0D;
        double norm = 0.0D;
        double cell = cellSize;
        long oct = seed;
        for (int i = 0; i < octaves; i++) {
            total += SoftMixNoise.valueNoise(x, z, cell, oct) * amplitude;
            norm += amplitude;
            amplitude *= gain;
            cell /= lacunarity;
            oct = oct * 0x9E3779B97F4A7C15L + 0x7F4A7C15L;
        }
        return norm <= 0.0D ? 0.0D : total / norm;
    }

    /**
     * Ridged multifractal of the shared value-noise family in [0, 1]. The ridges of
     * {@code 1 - |fbm|} form branching, dendritic valley networks — organic fissure
     * skeletons that never read as the parallel stripes or clean contours a single
     * thresholded octave produces.
     */
    public static double ridgedFbm(double x, double z, double cellSize, int octaves, double gain,
                                   double lacunarity, long seed) {
        double amplitude = 1.0D;
        double total = 0.0D;
        double norm = 0.0D;
        double cell = cellSize;
        long oct = seed;
        for (int i = 0; i < octaves; i++) {
            double n = 1.0D - Math.abs(SoftMixNoise.valueNoise(x, z, cell, oct));
            total += n * n * amplitude;
            norm += amplitude;
            amplitude *= gain;
            cell /= lacunarity;
            oct = oct * 0x9E3779B97F4A7C15L + 0x7F4A7C15L;
        }
        return norm <= 0.0D ? 0.0D : clamp01(total / norm);
    }

    /** Deterministic unit value in [0, 1) from a cell hash. */
    public static double unitHash(int cellX, int cellZ, long seed) {
        long mixed = SoftMixNoise.mix(cellX, cellZ, seed);
        return ((mixed >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
    }

    /** Linear interpolation, mirroring {@link SoftMixNoise#lerp}. */
    public static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }
}
