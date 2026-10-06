package com.dexer.aquanaut.common.worldgen.blend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnIslandMaskTest {
    private static final double EPS = 1e-9;
    /** The water world preset's sea level; the plateau must stand clear above it. */
    private static final int SEA_LEVEL = 63;
    /** A representative raw cavity floor of a covered column near the origin. */
    private static final double RAW_FLOOR = -30.0D;

    @Test
    void plateauIsFullyBlendedNearSpawn() {
        assertEquals(1.0D, SpawnIslandMask.maskAt(0, 0), EPS);
        assertEquals(1.0D, SpawnIslandMask.maskAt(SpawnIslandMask.FULL_RADIUS - 1, 0), EPS);
        assertEquals(1.0D, SpawnIslandMask.maskAt(0, -(SpawnIslandMask.FULL_RADIUS - 1)), EPS);
        assertEquals(1.0D, SpawnIslandMask.maskAt(33, 33), EPS);
    }

    @Test
    void maskFadesMonotonicallyToZeroAtTheOuterRadius() {
        double previous = Double.MAX_VALUE;
        for (int x = 0; x <= SpawnIslandMask.FADE_RADIUS + 64; x += 7) {
            double mask = SpawnIslandMask.maskAt(x, 0);
            assertTrue(mask <= previous + EPS, "mask grew along the ray at x=" + x);
            assertTrue(mask >= 0.0D && mask <= 1.0D, "mask out of range at x=" + x);
            previous = mask;
        }
        assertEquals(0.0D, SpawnIslandMask.maskAt(SpawnIslandMask.FADE_RADIUS, 0), EPS);
        assertEquals(0.0D, SpawnIslandMask.maskAt(SpawnIslandMask.FADE_RADIUS + 500, -1000), EPS);
    }

    @Test
    void maskIsSymmetricAroundTheSpawnColumn() {
        double reference = SpawnIslandMask.maskAt(96, 130);
        assertEquals(reference, SpawnIslandMask.maskAt(-96, 130), EPS);
        assertEquals(reference, SpawnIslandMask.maskAt(96, -130), EPS);
        assertEquals(reference, SpawnIslandMask.maskAt(-96, -130), EPS);
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
        for (int x = -SpawnIslandMask.FULL_RADIUS; x <= SpawnIslandMask.FULL_RADIUS; x += 8) {
            for (int z = -SpawnIslandMask.FULL_RADIUS; z <= SpawnIslandMask.FULL_RADIUS; z += 8) {
                double floor = SpawnIslandMask.blendFloor(RAW_FLOOR, SpawnIslandMask.maskAt(x, z));
                assertTrue(floor > SEA_LEVEL,
                        "island floor underwater at (" + x + ", " + z + "): " + floor);
            }
        }
    }

    @Test
    void floorSlopeStaysGentleThroughTheFade() {
        double previous = SpawnIslandMask.blendFloor(RAW_FLOOR, SpawnIslandMask.maskAt(0, 0));
        for (int x = 1; x <= SpawnIslandMask.FADE_RADIUS; x++) {
            double floor = SpawnIslandMask.blendFloor(RAW_FLOOR, SpawnIslandMask.maskAt(x, 0));
            assertTrue(Math.abs(floor - previous) <= 1.2D,
                    "cliff in the blended island flank at x=" + x);
            previous = floor;
        }
    }
}
