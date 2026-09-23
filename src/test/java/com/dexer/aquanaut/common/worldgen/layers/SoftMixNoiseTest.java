package com.dexer.aquanaut.common.worldgen.layers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SoftMixNoiseTest {

    @Test
    void softBinaryWeightsSumToOne() {
        for (double noise = -1.0; noise <= 1.0; noise += 0.1) {
            double[] weights = SoftMixNoise.softBinaryWeights(noise, 0.2);
            assertEquals(1.0, weights[0] + weights[1], 1e-9);
            assertTrue(weights[0] >= 0 && weights[1] >= 0);
        }
    }

    @Test
    void hardBlendKeepsStepAtZero() {
        double[] left = SoftMixNoise.softBinaryWeights(-0.1, 0.0);
        double[] right = SoftMixNoise.softBinaryWeights(0.1, 0.0);
        assertEquals(1.0, left[0], 1e-9);
        assertEquals(1.0, right[1], 1e-9);
    }

    @Test
    void softBlendIsContinuousAcrossZero() {
        double[] left = SoftMixNoise.softBinaryWeights(-0.05, 0.3);
        double[] mid = SoftMixNoise.softBinaryWeights(0.0, 0.3);
        double[] right = SoftMixNoise.softBinaryWeights(0.05, 0.3);
        assertTrue(left[1] < mid[1] && mid[1] < right[1], "jelly weight should rise smoothly");
        assertTrue(left[1] > 0.0 && right[1] < 1.0, "border should be a mix, not a snap");
    }

    @Test
    void bandWeightIsOneInsideAndZeroOutside() {
        assertEquals(1.0, SoftMixNoise.bandWeight(35, 32, 39, 2, 2), 1e-6);
        assertEquals(0.0, SoftMixNoise.bandWeight(20, 32, 39, 2, 2), 1e-6);
        assertEquals(0.0, SoftMixNoise.bandWeight(50, 32, 39, 2, 2), 1e-6);
    }

    @Test
    void bandWeightBlendsAtEdges() {
        double lower = SoftMixNoise.bandWeight(31, 32, 39, 2, 2);
        double inside = SoftMixNoise.bandWeight(34, 32, 39, 2, 2);
        double upper = SoftMixNoise.bandWeight(38, 32, 39, 2, 2);
        assertTrue(lower > 0.0 && lower < 1.0, "lower edge should blend");
        assertTrue(inside > 0.9, "core should be near full weight");
        assertTrue(upper > 0.0 && upper < 1.0, "upper edge should blend");
    }

    @Test
    void valueNoiseIsLocallyCoherent() {
        double a = SoftMixNoise.valueNoise(0, 0, 32, 2L);
        double b = SoftMixNoise.valueNoise(2, 2, 32, 2L);
        assertTrue(Math.abs(a - b) < 0.25, "nearby samples in the same patch should be close");
    }
}
