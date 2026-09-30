package com.dexer.aquanaut.common.worldgen.blend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests of the individual mix strategies: emergence grows from zero, karst
 * dissolution pinches shut at district borders, the contact dither interbeds families in
 * proportion to the weights, height composition stays inside the convex hull, and the
 * boundary warp is bounded and non-degenerate.
 */
public final class MixStrategiesTest {

    @Test
    void emergenceStartsAtZeroAndIsMonotone() {
        EmergenceCurve curve = EmergenceCurve.DEFAULT_EDIFICE;
        assertEquals(0.0D, curve.apply(0.0D), 1e-12);
        assertEquals(1.0D, curve.apply(1.0D), 1e-12);
        double previous = -1.0D;
        for (double w = 0.0D; w <= 1.0D; w += 0.01D) {
            double value = curve.apply(w);
            assertTrue(value >= previous - 1e-12, "emergence never retreats");
            assertTrue(value >= 0.0D && value <= 1.0D);
            previous = value;
        }
        // C1 at the window start: the first sliver of weight adds near-zero relief.
        assertTrue(curve.apply(0.01D) < 0.01D, "emergence leaves the plain with zero slope");
    }

    @Test
    void karstDissolutionSealsAndOpensAtTheExtremes() {
        DissolutionField.Karst karst = DissolutionField.Karst.DEFAULT;
        for (double noise = 0.0D; noise <= 1.0D; noise += 0.1D) {
            assertEquals(0.0D, karst.openness(0.0D, noise, 0.0D), 1e-12,
                    "solid districts never dissolve");
            assertEquals(1.0D, karst.openness(1.0D, noise, 0.0D), 1e-12,
                    "the crystal-nest core is fully open");
        }
    }

    @Test
    void karstDissolutionIsContinuousAndMonotoneInTheDistrictWeight() {
        DissolutionField.Karst karst = DissolutionField.Karst.DEFAULT;
        double previous = -1.0D;
        for (double voidAmount = 0.0D; voidAmount <= 1.0D; voidAmount += 0.01D) {
            double openness = karst.openness(voidAmount, 0.5D, 0.0D);
            assertTrue(openness >= previous - 1e-12, "more void appetite never seals rock back up");
            previous = openness;
        }
        // Intermediate weights give partial, speckled dissolution — the karst window phase.
        double mid = karst.openness(0.5D, 0.6D, 0.0D);
        assertTrue(mid > 0.0D && mid < 1.0D,
                "half-solid rock dissolves in windows instead of all-or-nothing (" + mid + ")");
        // Noise modulates: higher ridges open sooner, so borders are irregular.
        assertTrue(karst.openness(0.4D, 0.9D, 0.0D) > karst.openness(0.4D, 0.1D, 0.0D),
                "the noise field modulates where the rock gives way");
        // Depth bias widens fissures downward.
        assertTrue(karst.openness(0.3D, 0.5D, 0.3D) >= karst.openness(0.3D, 0.5D, 0.0D));
    }

    @Test
    void contactDitherFollowsTheWeights() {
        ContactMaterial.HashDither dither = ContactMaterial.HashDither.DEFAULT;
        double[] weights = {0.5D, 0.5D, 0.0D};
        int[] counts = new int[3];
        int samples = 0;
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 8; y++) {
                for (int z = 0; z < 64; z++) {
                    counts[dither.pickFamily(weights, x, y, z, 99L)]++;
                    samples++;
                }
            }
        }
        double share0 = counts[0] / (double) samples;
        double share1 = counts[1] / (double) samples;
        assertEquals(0.0D, counts[2] / (double) samples, 1e-12, "zero-weight families never appear");
        assertTrue(share0 > 0.3D && share0 < 0.7D,
                "a 50/50 contact interbeds both families (" + share0 + "/" + share1 + ")");
        assertTrue(share1 > 0.3D && share1 < 0.7D);
    }

    @Test
    void contactDitherKeepsNearPureDistrictsClean() {
        ContactMaterial.HashDither dither = ContactMaterial.HashDither.DEFAULT;
        double[] weights = {0.97D, 0.03D};
        int minority = 0;
        int samples = 0;
        for (int x = 0; x < 48; x++) {
            for (int y = 0; y < 6; y++) {
                for (int z = 0; z < 48; z++) {
                    if (dither.pickFamily(weights, x, y, z, 7L) == 1) {
                        minority++;
                    }
                    samples++;
                }
            }
        }
        double share = minority / (double) samples;
        assertTrue(share < 0.02D,
                "gamma sharpening keeps a near-pure district free of confetti (" + share + ")");
    }

    @Test
    void contactDitherIsDeterministicAndCellular() {
        ContactMaterial.HashDither dither = ContactMaterial.HashDither.DEFAULT;
        double[] weights = {0.55D, 0.45D};
        int first = dither.pickFamily(weights, 10, 5, -3, 123L);
        for (int i = 0; i < 5; i++) {
            assertEquals(first, dither.pickFamily(weights, 10, 5, -3, 123L),
                    "the same block always gets the same family");
        }
        // Same 2-block cell → same family (patches, not per-block confetti).
        assertEquals(dither.pickFamily(weights, 10, 5, -3, 123L),
                dither.pickFamily(weights, 11, 5, -4, 123L));
    }

    @Test
    void heightCombinersStayInsideTheHull() {
        double[] values = {-4.0D, 9.0D, 2.0D};
        double[] weights = {0.2D, 0.5D, 0.3D};
        double linear = HeightCombiner.LINEAR.combine(values, weights);
        assertEquals(-4.0D * 0.2D + 9.0D * 0.5D + 2.0D * 0.3D, linear, 1e-9);
        assertEquals(9.0D, HeightCombiner.MAX.combine(values, weights), 1e-12);
        double soft = HeightCombiner.softMax(4.0D).combine(values, weights);
        assertTrue(soft <= 9.0D + 1e-9 && soft >= 9.0D + Math.log(0.5D) / 4.0D - 1e-9,
                "soft max hugs the hard max from below (" + soft + ")");
        // Zero weights fall back gracefully instead of NaN.
        double fallback = HeightCombiner.LINEAR.combine(values, new double[]{0.0D, 0.0D, 0.0D});
        assertTrue(Double.isFinite(fallback));
    }

    @Test
    void boundaryWarpIsBoundedAndNonDegenerate() {
        BoundaryWarp.Fbm warp = new BoundaryWarp.Fbm(3.2D, 24.0D, 7.0D, 0xB1E4D5EEDL);
        double maxShift = 0.0D;
        double distinct = 0.0D;
        double previous = Double.NaN;
        for (int q = -200; q <= 200; q++) {
            double dx = warp.warpX(q, 13);
            double dz = warp.warpZ(q, 13);
            maxShift = Math.max(maxShift, Math.max(Math.abs(dx), Math.abs(dz)));
            if (!Double.isNaN(previous) && Math.abs(dx - previous) > 1e-9) {
                distinct++;
            }
            previous = dx;
        }
        assertTrue(maxShift <= 3.2D + 1e-9, "warp stays inside its amplitude budget (" + maxShift + ")");
        assertTrue(distinct > 100, "warp actually moves the sample point around");
        assertEquals(0.0D, BoundaryWarp.IDENTITY.warpX(5, 9), 0.0D);
        assertEquals(0.0D, BoundaryWarp.IDENTITY.warpZ(5, 9), 0.0D);
    }
}
