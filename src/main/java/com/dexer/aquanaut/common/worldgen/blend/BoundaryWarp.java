package com.dexer.aquanaut.common.worldgen.blend;

import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;

/**
 * Strategy: domain warp applied to sample coordinates before a boundary or fissure noise
 * field is evaluated. Warping turns the smooth, evenly-drawn contours of a single noise
 * octave into meandering, pinching and branching lines — the difference between a border
 * that reads as a coastline and one that reads as a drawn contour. This is the general
 * replacement for the "very regular cracks" look of axis-aligned or single-octave
 * threshold fields.
 */
public interface BoundaryWarp {
    /** Warp component along X for a sample position. */
    double warpX(double x, double z);

    /** Warp component along Z for a sample position. */
    double warpZ(double x, double z);

    default double[] warp(double x, double z, double[] out) {
        out[0] = x + warpX(x, z);
        out[1] = z + warpZ(x, z);
        return out;
    }

    /** No warp — fields sample the raw lattice. */
    BoundaryWarp IDENTITY = new BoundaryWarp() {
        @Override
        public double warpX(double x, double z) {
            return 0.0D;
        }

        @Override
        public double warpZ(double x, double z) {
            return 0.0D;
        }
    };

    /**
     * Two-octave fractal warp: a broad bend plus a fine wiggle, offsets in the same units
     * as the sample coordinates. Amplitude scales linearly, so amplitude 0 degenerates to
     * {@link #IDENTITY}.
     */
    record Fbm(double amplitude, double broadCell, double fineCell, long seed) implements BoundaryWarp {
        public static final double FINE_SHARE = 0.30D;

        public Fbm {
            if (broadCell <= 0.0D || fineCell <= 0.0D) {
                throw new IllegalArgumentException("warp cells must be positive");
            }
        }

        @Override
        public double warpX(double x, double z) {
            return offset(x, z, seed);
        }

        @Override
        public double warpZ(double x, double z) {
            return offset(x, z, seed ^ 0x9E3779B97F4A7C15L);
        }

        private double offset(double x, double z, long octSeed) {
            if (amplitude == 0.0D) {
                return 0.0D;
            }
            double broad = SoftMixNoise.valueNoise(x, z, broadCell, octSeed);
            double fine = SoftMixNoise.valueNoise(x, z, fineCell, octSeed ^ 0x51EDL);
            return amplitude * ((1.0D - FINE_SHARE) * broad + FINE_SHARE * fine);
        }
    }
}
