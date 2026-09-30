package com.dexer.aquanaut.common.worldgen.blend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guarantees of the shared blend maths: C1 taper windows, bounded fractal fields and a
 * smooth maximum that never undercuts the hard maximum.
 */
public final class BlendMathTest {

    @Test
    void taperIsC1AtTheWindowEdge() {
        assertEquals(0.0D, BlendMath.taper(1.0D), 1e-12);
        assertEquals(0.0D, BlendMath.taper(-1.0D), 1e-12);
        assertEquals(1.0D, BlendMath.taper(0.0D), 1e-12);
        // Zero derivative at the edge: the last epsilon inside the window contributes
        // a sub-milli block of relief, so no cliff step survives at the band border.
        double epsilon = 1e-4;
        assertTrue(BlendMath.taper(1.0D - epsilon) < 1e-9,
                "taper must approach zero with zero slope, got " + BlendMath.taper(1.0D - epsilon));
        assertTrue(BlendMath.taper(-1.0D + epsilon) < 1e-9);
        // Monotone halves.
        double previous = 1.0D;
        for (double u = 0.0D; u <= 1.0D; u += 0.05D) {
            double value = BlendMath.taper(u);
            assertTrue(value <= previous + 1e-12, "taper falls monotonically toward the edge");
            previous = value;
        }
    }

    @Test
    void quinticInterpolationIsSmoothAndExact() {
        assertEquals(0.0D, BlendMath.quintic(0.0D), 1e-12);
        assertEquals(1.0D, BlendMath.quintic(1.0D), 1e-12);
        assertEquals(0.5D, BlendMath.quintic(0.5D), 1e-9);
        // Zero initial slope: interpolated fields leave lattice values without a kink.
        assertTrue(BlendMath.quintic(1e-4) < 1e-9);
        assertTrue(1.0D - BlendMath.quintic(1.0D - 1e-4) < 1e-9);
        double previous = -1.0D;
        for (double t = 0.0D; t <= 1.0D; t += 0.02D) {
            double value = BlendMath.quintic(t);
            assertTrue(value >= previous, "quintic is monotone");
            previous = value;
        }
    }

    @Test
    void softMaxStaysBetweenMaxAndMaxPlusEpsilon() {
        double[] values = {3.0D, 7.0D, -2.0D};
        double[] weights = {0.5D, 0.3D, 0.2D};
        double composed = BlendMath.softMax(values, weights, 4.0D);
        // The weighted smooth max approaches the hard max from below, never above it, and
        // never undercuts it by more than ln(W/w_max)/p.
        assertTrue(composed <= 7.0D + 1e-9,
                "smooth max never overtops the hard max (" + composed + ")");
        assertTrue(composed >= 7.0D + Math.log(0.3D) / 4.0D - 1e-9,
                "smooth max stays close to the hard max (" + composed + ")");
        // p → 0 degenerates to the hard max.
        assertEquals(7.0D, BlendMath.softMax(values, weights, 0.0D), 1e-12);
        // Higher p hugs the max tighter.
        double loose = BlendMath.softMax(values, weights, 1.0D);
        double tight = BlendMath.softMax(values, weights, 16.0D);
        assertTrue(tight >= loose - 1e-9 && tight <= 7.0D + 1e-9,
                "sharper p approaches the hard max (" + loose + " -> " + tight + ")");
    }

    @Test
    void softMaxIsContinuousInTheInputs() {
        double[] weights = {0.5D, 0.5D};
        double previous = BlendMath.softMax(new double[]{0.0D, 0.0D}, weights, 4.0D);
        for (double v = 0.05D; v <= 6.0D; v += 0.05D) {
            double composed = BlendMath.softMax(new double[]{0.0D, v}, weights, 4.0D);
            assertTrue(Math.abs(composed - previous) < 0.2D,
                    "smooth max has no kink cliff at v=" + v);
            previous = composed;
        }
    }

    @Test
    void fractalFieldsStayInRange() {
        for (int x = -200; x <= 200; x += 17) {
            for (int z = -200; z <= 200; z += 19) {
                double fbm = BlendMath.fbm(x, z, 32.0D, 3, 0.5D, 2.0D, 1234L);
                assertTrue(fbm >= -1.0D && fbm <= 1.0D, "fbm stays in [-1, 1] (" + fbm + ")");
                double ridged = BlendMath.ridgedFbm(x, z, 9.0D, 2, 0.55D, 2.1D, 4321L);
                assertTrue(ridged >= 0.0D && ridged <= 1.0D, "ridged fbm stays in [0, 1] (" + ridged + ")");
            }
        }
    }

    @Test
    void ridgedFbmHasInteriorVariation() {
        // A karst modulation field that never varies would dissolve rock in plain discs;
        // require real spatial variation across a district border scale.
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int x = 0; x < 128; x += 3) {
            for (int z = 0; z < 128; z += 3) {
                double ridged = BlendMath.ridgedFbm(x, z, 9.0D, 2, 0.55D, 2.1D, 4321L);
                min = Math.min(min, ridged);
                max = Math.max(max, ridged);
            }
        }
        assertTrue(max - min > 0.3D, "ridged field spans a useful range (" + min + ".." + max + ")");
    }

    @Test
    void unitHashStaysInUnitRangeAndVaries() {
        int low = 0;
        int high = 0;
        for (int i = -500; i < 500; i++) {
            double unit = BlendMath.unitHash(i, i * 7 + 3, 0xDEADL);
            assertTrue(unit >= 0.0D && unit < 1.0D);
            if (unit < 0.5D) {
                low++;
            } else {
                high++;
            }
        }
        assertTrue(low > 300 && high > 300, "hash draws spread across the unit interval");
    }
}
