package com.dexer.aquanaut.common.worldgen.blend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnIslandMaskTest {
    private static final double EPS = 1e-9;
    /** The water world preset's sea level; the plateau must stand clear above it. */
    private static final int SEA_LEVEL = 63;
    /** A representative raw cavity floor of a covered column near the origin. */
    private static final double RAW_FLOOR = -30.0D;
    /** comfortably inside the tightest bay of the randomized plateau. */
    private static final int SAFE_PLATEAU_RADIUS = 32;

    @Test
    void plateauIsFullyBlendedNearSpawn() {
        assertEquals(1.0D, SpawnIslandMask.maskAt(0, 0), EPS);
        // Every direction around the spawn chunk, well inside the tightest bay.
        for (int deg = 0; deg < 360; deg += 10) {
            double rad = Math.toRadians(deg);
            int x = (int) Math.round(Math.cos(rad) * SAFE_PLATEAU_RADIUS);
            int z = (int) Math.round(Math.sin(rad) * SAFE_PLATEAU_RADIUS);
            assertEquals(1.0D, SpawnIslandMask.maskAt(x, z), EPS,
                    "plateau not fully emerged at " + deg + " deg");
        }
    }

    @Test
    void coastlinesStayAboveSeaLevelEvenInTheTightestBay() {
        assertTrue(SpawnIslandMask.MIN_PLATEAU_RADIUS > SAFE_PLATEAU_RADIUS,
                "safe plateau radius must be inside the minimum coastline");
        double floor = SpawnIslandMask.blendFloor(RAW_FLOOR, SpawnIslandMask.maskAt(SAFE_PLATEAU_RADIUS, 0));
        assertEquals(SpawnIslandMask.ISLAND_TOP_Y, floor, EPS);
        assertTrue(floor > SEA_LEVEL);
    }

    @Test
    void maskFadesMonotonicallyAlongEveryRay() {
        int limit = (int) Math.ceil(SpawnIslandMask.MAX_FADE_RADIUS) + 64;
        for (int deg = 0; deg < 180; deg += 15) {
            double rad = Math.toRadians(deg);
            double ux = Math.cos(rad);
            double uz = Math.sin(rad);
            double previous = Double.MAX_VALUE;
            for (int d = 0; d <= limit; d += 7) {
                int x = (int) Math.round(ux * d);
                int z = (int) Math.round(uz * d);
                double mask = SpawnIslandMask.maskAt(x, z);
                assertTrue(mask <= previous + EPS, "mask grew along the " + deg + " deg ray at d=" + d);
                assertTrue(mask >= 0.0D && mask <= 1.0D, "mask out of range on the " + deg + " deg ray");
                previous = mask;
            }
        }
    }

    @Test
    void liftIsZeroBeyondTheLongestPeninsulaAndPositiveOnTheFarRing() {
        // The far ring at radius 180 sits inside every direction's fade, so water world's
        // stack-support override keeps claiming columns all around the island.
        for (int deg = 0; deg < 360; deg += 10) {
            double rad = Math.toRadians(deg);
            int x = (int) Math.round(Math.cos(rad) * 180);
            int z = (int) Math.round(Math.sin(rad) * 180);
            assertTrue(SpawnIslandMask.maskAt(x, z) > 0.0D, "far ring dry at " + deg + " deg");
        }
        // Past the longest possible peninsula the lift is exactly zero.
        int beyond = (int) Math.ceil(SpawnIslandMask.MAX_FADE_RADIUS) + 8;
        assertEquals(0.0D, SpawnIslandMask.maskAt(beyond, 0), EPS);
        assertEquals(0.0D, SpawnIslandMask.maskAt(-beyond, -beyond), EPS);
    }

    @Test
    void coastlineIsContinuousAroundTheOriginAndActuallyRandom() {
        // Walking a circle at the mean plateau radius must not jump: the noise is sampled on
        // the unit circle, so the outline wraps the ±pi seam without tearing.
        double previous = SpawnIslandMask.maskAt(SpawnIslandMask.FULL_RADIUS, 0);
        double first = previous;
        int samples = 180;
        double changedDirections = 0;
        for (int i = 1; i <= samples; i++) {
            double rad = 2.0D * Math.PI * i / samples;
            int x = (int) Math.round(Math.cos(rad) * SpawnIslandMask.FULL_RADIUS);
            int z = (int) Math.round(Math.sin(rad) * SpawnIslandMask.FULL_RADIUS);
            double mask = SpawnIslandMask.maskAt(x, z);
            assertTrue(Math.abs(mask - previous) <= 0.05D,
                    "coastline jumped at sample " + i);
            if (Math.abs(mask - first) > 1.0e-6D) {
                changedDirections++;
            }
            previous = mask;
        }
        // The outline is genuinely irregular: the ring leaves the plateau in some directions.
        assertTrue(changedDirections > 0, "coastline modulation never leaves the mean radius");
        assertNotEquals(SpawnIslandMask.maskAt(60, 0), SpawnIslandMask.maskAt(0, 60), EPS);
    }

    @Test
    void blendFloorNeverLowersGround() {
        double[] raws = {-40.0D, -20.0D, 0.0D, 35.0D, SpawnIslandMask.ISLAND_TOP_Y,
                SpawnIslandMask.ISLAND_TOP_Y + 15.0D};
        for (double raw : raws) {
            assertEquals(raw, SpawnIslandMask.blendFloor(raw, 0.0D), EPS);
            assertEquals(Math.max(raw, SpawnIslandMask.ISLAND_TOP_Y),
                    SpawnIslandMask.blendFloor(raw, 1.0D), EPS);
            for (int x = 0; x <= SpawnIslandMask.FADE_RADIUS; x += 11) {
                assertTrue(SpawnIslandMask.blendFloor(raw, SpawnIslandMask.maskAt(x, 0)) >= raw - EPS,
                        "blended floor sank below the raw floor at x=" + x);
            }
        }
    }

    @Test
    void plateauStandsAboveSeaLevel() {
        for (int x = -SAFE_PLATEAU_RADIUS; x <= SAFE_PLATEAU_RADIUS; x += 8) {
            for (int z = -SAFE_PLATEAU_RADIUS; z <= SAFE_PLATEAU_RADIUS; z += 8) {
                double floor = SpawnIslandMask.blendFloor(RAW_FLOOR, SpawnIslandMask.maskAt(x, z));
                assertTrue(floor > SEA_LEVEL,
                        "island floor underwater at (" + x + ", " + z + "): " + floor);
            }
        }
    }

    @Test
    void floorSlopeStaysGentleThroughTheFade() {
        // The raw field's steepest smoothstep flank swings ~100 * 1.5 / 144 ≈ 1.04 blocks per
        // block, and integer ray sampling wobbles the coastline direction a little on top.
        // This test guards against tearing (multi-block jumps), not against the gentle ramp —
        // relaxing slopes beyond 1.0 is CliffGuard's contract on the guarded grid.
        for (int deg = 0; deg < 180; deg += 22) {
            double rad = Math.toRadians(deg);
            double ux = Math.cos(rad);
            double uz = Math.sin(rad);
            double previous = SpawnIslandMask.blendFloor(RAW_FLOOR, SpawnIslandMask.maskAt(0, 0));
            for (int d = 1; d <= (int) Math.ceil(SpawnIslandMask.MAX_FADE_RADIUS); d++) {
                int x = (int) Math.round(ux * d);
                int z = (int) Math.round(uz * d);
                double floor = SpawnIslandMask.blendFloor(RAW_FLOOR, SpawnIslandMask.maskAt(x, z));
                assertTrue(Math.abs(floor - previous) <= 2.0D,
                        "cliff in the blended island flank on the " + deg + " deg ray at d=" + d);
                previous = floor;
            }
        }
    }
}
