package com.dexer.aquanaut.common.worldgen.layers;

/**
 * Shared value-noise helpers for soft horizontal biome mixes and layer blends.
 */
public final class SoftMixNoise {
    private SoftMixNoise() {
    }

    public static double valueNoise(int x, int z, int cellSize, long seed) {
        return valueNoise((double) x, (double) z, (double) cellSize, seed);
    }

    /**
     * Continuous-coordinate variant of the same value-noise lattice. Integer coordinates and
     * cell sizes sample exactly like {@link #valueNoise(int, int, int, long)}, but warped or
     * interpolated sample points land between lattice cells instead of snapping to them, so
     * domain-warped fields stay continuous.
     */
    public static double valueNoise(double x, double z, double cellSize, long seed) {
        if (cellSize <= 0.0D) {
            return 0.0D;
        }
        double cellX = x / cellSize;
        double cellZ = z / cellSize;
        int baseX = (int) Math.floor(cellX);
        int baseZ = (int) Math.floor(cellZ);
        double sx = smoothstep(cellX - baseX);
        double sz = smoothstep(cellZ - baseZ);

        double n00 = cornerNoise(baseX, baseZ, seed);
        double n10 = cornerNoise(baseX + 1, baseZ, seed);
        double n01 = cornerNoise(baseX, baseZ + 1, seed);
        double n11 = cornerNoise(baseX + 1, baseZ + 1, seed);
        double nx0 = lerp(sx, n00, n10);
        double nx1 = lerp(sx, n01, n11);
        return lerp(sz, nx0, nx1);
    }

    /**
     * Soft assignment of a noise field in [-1, 1] into two complementary weights.
     * {@code blend} is the half-width of the transition in noise units (0 = hard cut).
     */
    public static double[] softBinaryWeights(double noise, double blend) {
        double t = softThreshold(noise, blend);
        return new double[]{1.0D - t, t};
    }

    /**
     * Smoothstep remap of noise through zero. blend=0 keeps a hard step at 0.
     */
    public static double softThreshold(double noise, double blend) {
        if (blend <= 0.0D) {
            return noise >= 0.0D ? 1.0D : 0.0D;
        }
        double t = (noise + blend) / (2.0D * blend);
        return smoothstep(clamp01(t));
    }

    /**
     * Vertical membership weight for a closed [minY, maxY] band with optional edge blend in the same units as Y.
     */
    public static double bandWeight(double y, double minY, double maxY, double blendDown, double blendUp) {
        double rise = blendUp <= 0.0D
                ? (y <= maxY ? 1.0D : 0.0D)
                : 1.0D - smoothstep(clamp01((y - (maxY - blendUp)) / blendUp));
        double fall = blendDown <= 0.0D
                ? (y >= minY ? 1.0D : 0.0D)
                : smoothstep(clamp01((y - (minY - blendDown)) / blendDown));
        return clamp01(rise * fall);
    }

    public static double smoothstep(double value) {
        double t = clamp01(value);
        return t * t * (3.0D - 2.0D * t);
    }

    public static double clamp01(double value) {
        if (value <= 0.0D) {
            return 0.0D;
        }
        if (value >= 1.0D) {
            return 1.0D;
        }
        return value;
    }

    public static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    public static double cornerNoise(int cellX, int cellZ, long seed) {
        long hash = mix(cellX, cellZ, seed);
        return (Math.floorMod(hash, 2001L) / 1000.0D) - 1.0D;
    }

    public static long mix(int cellX, int cellZ, long salt) {
        long value = 0x9E3779B97F4A7C15L;
        value ^= (long) cellX * 341873128712L;
        value ^= (long) cellZ * 132897987541L;
        value ^= salt * 0x94D049BB133111EBL;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
