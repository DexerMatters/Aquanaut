package com.dexer.aquanaut.common.worldgen.blend;

import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Invariants of the anti-cliff guard: the relaxation re-slopes cliffs to the guard step,
 * flat ground passes through untouched, the guard never wanders far from the geology, and
 * — because every pass is a fixed-radius local operator — the same world column guards
 * bit-identically no matter which chunk's grid computes it.
 */
public final class CliffGuardTest {
    private static final CliffGuard.ConeErosion GUARD = new CliffGuard.ConeErosion(1.0D, 2.0D, 12);

    /** Adversarial field: an 8-block vertical cliff with gentle noise on both plates. */
    private static double cliffField(int x, int z) {
        double base = x < 0 ? 0.0D : 8.0D;
        return base + SoftMixNoise.valueNoise(x, z, 24, 777L) * 2.0D;
    }

    private static double[] sampleGrid(int originX, int originZ, int extent) {
        double[] raw = new double[extent * extent];
        for (int gx = 0; gx < extent; gx++) {
            for (int gz = 0; gz < extent; gz++) {
                raw[gx * extent + gz] = cliffField(originX + gx, originZ + gz);
            }
        }
        return raw;
    }

    @Test
    void guardRemovesTheCliffEntirely() {
        int extent = 40;
        double[] guarded = GUARD.guard(sampleGrid(-20, -20, extent), extent);
        int lo = GUARD.radius();
        int hi = extent - GUARD.radius() - 1;
        double worstStep = 0.0D;
        for (int x = lo; x < hi; x++) {
            for (int z = lo; z < hi; z++) {
                double here = guarded[x * extent + z];
                double east = guarded[(x + 1) * extent + z];
                double south = guarded[x * extent + z + 1];
                double diag = guarded[(x + 1) * extent + z + 1];
                worstStep = Math.max(worstStep, Math.abs(east - here));
                worstStep = Math.max(worstStep, Math.abs(south - here));
                worstStep = Math.max(worstStep, Math.abs(diag - here));
            }
        }
        // The 8-block raw cliff comes out as a ramp at the guard slope; the only slack is
        // where the deviation floor catches the relaxation (bounded by the smoothed
        // geology's own slope), so the guarantee is a small multiple of maxStep — never
        // the wall that went in.
        assertTrue(worstStep <= 1.5D + 1e-9,
                "an 8-block raw cliff must come out ramped, worst adjacent step " + worstStep);
    }

    @Test
    void flatGroundPassesThroughUnchanged() {
        int extent = 32;
        double[] raw = new double[extent * extent];
        java.util.Arrays.fill(raw, 42.0D);
        double[] guarded = GUARD.guard(raw, extent);
        for (int x = GUARD.radius(); x < extent - GUARD.radius(); x++) {
            for (int z = GUARD.radius(); z < extent - GUARD.radius(); z++) {
                assertEquals(42.0D, guarded[x * extent + z], 1e-9,
                        "flat terrain is a fixed point of the guard");
            }
        }
    }

    @Test
    void guardStaysNearTheGeologyAwayFromDiscontinuities() {
        int extent = 56;
        double[] raw = sampleGrid(-28, -28, extent);
        double[] guarded = GUARD.guard(raw, extent);
        // Well away from the cliff line (beyond the relaxation reach) the deviation floor
        // dominates: the guard may smooth but must not relocate the floor.
        int checked = 0;
        for (int x = GUARD.radius(); x < extent - GUARD.radius(); x++) {
            for (int z = GUARD.radius(); z < extent - GUARD.radius(); z++) {
                if (Math.abs(x - 28) < 12) {
                    continue;
                }
                double deviation = Math.abs(guarded[x * extent + z] - raw[x * extent + z]);
                assertTrue(deviation <= 2.0D + 1e-9,
                        "guard keeps the geological floor within maxDeviation (" + deviation + ")");
                checked++;
            }
        }
        assertTrue(checked > 50, "the far field must actually be sampled");
    }

    @Test
    void guardIsTranslationInvariantSoChunksCannotSeam() {
        int extent = 64;
        double[] windowA = GUARD.guard(sampleGrid(-28, -28, extent), extent);
        double[] windowB = GUARD.guard(sampleGrid(-12, -28, extent), extent);
        // A covers world -28..35 (valid interior -14..21), B covers -12..51 (valid 2..37):
        // shared valid columns must guard bit-identically from either grid.
        int compared = 0;
        for (int worldX = 2; worldX <= 21; worldX++) {
            for (int worldZ = 0; worldZ <= 15; worldZ++) {
                int ax = worldX + 28;
                int az = worldZ + 28;
                int bx = worldX + 12;
                int bz = worldZ + 28;
                assertEquals(windowA[ax * extent + az], windowB[bx * extent + bz], 1e-12,
                        "shared column (" + worldX + "," + worldZ + ") must guard identically");
                compared++;
            }
        }
        assertTrue(compared > 100, "the windows must actually overlap");
    }

    @Test
    void analyticGuardPointMatchesTheGridCentre() {
        int extent = 36;
        double[] raw = sampleGrid(-18, -18, extent);
        double[] guarded = GUARD.guard(raw, extent);
        CliffGuard.RawFloor rawFloor = (x, z) -> cliffField(x, z);
        int checked = 0;
        for (int worldX = -4; worldX <= 3; worldX++) {
            for (int worldZ = -4; worldZ <= 3; worldZ++) {
                double analytic = GUARD.guardPoint(rawFloor, worldX, worldZ);
                double grid = guarded[(worldX + 18) * extent + (worldZ + 18)];
                assertEquals(grid, analytic, 1e-9,
                        "analytic single-column path must equal the chunk grid at (" + worldX + "," + worldZ + ")");
                checked++;
            }
        }
        assertEquals(64, checked);
    }

    @Test
    void identityGuardPreservesInput() {
        double[] raw = {1.0D, 5.0D, 9.0D, 2.0D};
        double[] guarded = CliffGuard.IDENTITY.guard(raw, 2);
        assertEquals(1.0D, guarded[0], 0.0D);
        assertEquals(2.0D, guarded[3], 0.0D);
    }
}
