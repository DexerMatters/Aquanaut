package com.dexer.aquanaut.common.worldgen.blend;

import com.dexer.aquanaut.common.worldgen.layers.BiomeMix;
import com.dexer.aquanaut.common.worldgen.layers.OceanLayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The block-resolution district weight field must be a continuous, convex interpolation
 * of the quart lattice — no 4-block stair-steps, no weight leaks, and memoization must
 * never change a value.
 */
public final class DistrictWeightFieldTest {

    private static BiomeMix middleSea() {
        return OceanLayer.middleSea().mix();
    }

    @Test
    void weightsSumToOneEverywhere() {
        DistrictWeightField field = new DistrictWeightField(middleSea());
        double[] out = new double[middleSea().entries().size()];
        for (int x = -130; x <= 130; x += 7) {
            for (int z = -130; z <= 130; z += 11) {
                field.weightsAtBlock(x, z, out);
                double sum = 0.0D;
                for (double w : out) {
                    assertTrue(w >= 0.0D, "weights are non-negative");
                    sum += w;
                }
                assertEquals(1.0D, sum, 1e-9, "convex interpolation preserves the partition of unity");
            }
        }
    }

    @Test
    void adjacentBlocksNeverStairStep() {
        DistrictWeightField field = new DistrictWeightField(middleSea());
        double[] a = new double[middleSea().entries().size()];
        double[] b = new double[middleSea().entries().size()];
        double worst = 0.0D;
        for (int x = -64; x < 64; x++) {
            for (int z = -64; z < 64; z += 5) {
                field.weightsAtBlock(x, z, a);
                field.weightsAtBlock(x + 1, z, b);
                for (int i = 0; i < a.length; i++) {
                    worst = Math.max(worst, Math.abs(a[i] - b[i]));
                }
            }
        }
        // Quintic interpolation of a lattice whose neighbouring cells differ by at most 1
        // bounds the per-block slope at 1.875/4 ≈ 0.47.
        assertTrue(worst <= 0.5D,
                "block-to-block weight change stays small, worst " + worst);
    }

    @Test
    void blockFieldTracksTheQuartLattice() {
        BiomeMix mix = middleSea();
        DistrictWeightField field = new DistrictWeightField(mix);
        double[] out = new double[middleSea().entries().size()];
        for (int cellX = -8; cellX <= 8; cellX += 4) {
            for (int cellZ = -8; cellZ <= 8; cellZ += 4) {
                double[] quart = mix.weightsAt(cellX, cellZ);
                // The two central columns of the cell straddle the lattice node.
                field.weightsAtBlock(cellX * 4 + 1, cellZ * 4 + 1, out);
                for (int i = 0; i < out.length; i++) {
                    assertTrue(Math.abs(out[i] - quart[i]) < 0.05D,
                            "interpolated weights stay close to their lattice value at the cell centre");
                }
            }
        }
    }

    @Test
    void memoizationNeverChangesAValue() {
        BiomeMix mix = middleSea();
        DistrictWeightField memoized = new DistrictWeightField(mix, true);
        DistrictWeightField direct = new DistrictWeightField(mix, false);
        double[] a = new double[middleSea().entries().size()];
        double[] b = new double[middleSea().entries().size()];
        for (int x = -40; x <= 40; x += 3) {
            for (int z = -40; z <= 40; z += 7) {
                memoized.weightsAtBlock(x, z, a);
                direct.weightsAtBlock(x, z, b);
                assertArrayEquals(b, a, 1e-12,
                        "cached and uncached sampling must be bit-identical at (" + x + "," + z + ")");
            }
        }
        // Repeated reads through the memo stay stable too.
        memoized.weightsAtBlock(5, 9, a);
        memoized.weightsAtBlock(5, 9, b);
        assertArrayEquals(a, b, 0.0D);
    }

    @Test
    void singleEntryMixIsConstantOne() {
        DistrictWeightField field = new DistrictWeightField(OceanLayer.deepSea().mix());
        double[] out = new double[1];
        field.weightsAtBlock(1234, -5678, out);
        assertEquals(1.0D, out[0], 1e-12);
    }
}
