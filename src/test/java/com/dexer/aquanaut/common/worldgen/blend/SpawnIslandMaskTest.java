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
    /** Comfortably inside the tightest bay of any seed's randomized plateau. */
    private static final int SAFE_PLATEAU_RADIUS = 32;
    /** Every world seed the per-seed property tests walk. */
    private static final long[] SEEDS = {0L, 1L, 42L, -99L, 0x9E3779B97F4A7C15L};

    @Test
    void plateauIsFullyBlendedNearSpawn() {
        assertEquals(1.0D, SpawnIslandMask.maskAt(0, 0), EPS);
        // Every direction around the spawn chunk, well inside the tightest bay of any seed.
        for (long seed : SEEDS) {
            for (int deg = 0; deg < 360; deg += 10) {
                double rad = Math.toRadians(deg);
                int x = (int) Math.round(Math.cos(rad) * SAFE_PLATEAU_RADIUS);
                int z = (int) Math.round(Math.sin(rad) * SAFE_PLATEAU_RADIUS);
                assertEquals(1.0D, SpawnIslandMask.maskAt(seed, x, z), EPS,
                        "plateau not fully emerged, seed " + seed + ", " + deg + " deg");
            }
        }
    }

    @Test
    void coastlinesStayAboveSeaLevelEvenInTheTightestBay() {
        assertTrue(SpawnIslandMask.MIN_PLATEAU_RADIUS > SAFE_PLATEAU_RADIUS,
                "safe plateau radius must be inside the minimum coastline");
        double floor = SpawnIslandMask.blendFloor(RAW_FLOOR,
                SpawnIslandMask.maskAt(SAFE_PLATEAU_RADIUS, 0));
        assertEquals(SpawnIslandMask.ISLAND_TOP_Y, floor, EPS);
        assertTrue(floor > SEA_LEVEL);
    }

    @Test
    void maskFadesMonotonicallyAlongEveryRay() {
        int limit = (int) Math.ceil(SpawnIslandMask.MAX_FADE_RADIUS) + 64;
        for (long seed : SEEDS) {
            for (int deg = 0; deg < 180; deg += 15) {
                double rad = Math.toRadians(deg);
                double ux = Math.cos(rad);
                double uz = Math.sin(rad);
                double previous = Double.MAX_VALUE;
                for (int d = 0; d <= limit; d += 7) {
                    int x = (int) Math.round(ux * d);
                    int z = (int) Math.round(uz * d);
                    double mask = SpawnIslandMask.maskAt(seed, x, z);
                    assertTrue(mask <= previous + EPS,
                            "mask grew along the " + deg + " deg ray at d=" + d + ", seed " + seed);
                    assertTrue(mask >= 0.0D && mask <= 1.0D,
                            "mask out of range on the " + deg + " deg ray, seed " + seed);
                    previous = mask;
                }
            }
        }
    }

    @Test
    void liftIsZeroBeyondTheLongestPeninsulaAndPositiveOnTheFarRing() {
        // The far ring sits inside every seed's fade, so water world's stack-support override
        // keeps claiming columns all around the island.
        for (long seed : SEEDS) {
            for (int deg = 0; deg < 360; deg += 10) {
                double rad = Math.toRadians(deg);
                int x = (int) Math.round(Math.cos(rad) * 176);
                int z = (int) Math.round(Math.sin(rad) * 176);
                assertTrue(SpawnIslandMask.maskAt(seed, x, z) > 0.0D,
                        "far ring dry at " + deg + " deg, seed " + seed);
            }
        }
        // Past the longest possible peninsula of any seed the lift is exactly zero.
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
            assertTrue(Math.abs(mask - previous) <= 0.05D, "coastline jumped at sample " + i);
            if (Math.abs(mask - first) > 1.0e-6D) {
                changedDirections++;
            }
            previous = mask;
        }
        // The outline is genuinely irregular: a ring through the fade band leaves the
        // plateau-level blend in some directions but not others.
        int ring = SpawnIslandMask.FULL_RADIUS + SpawnIslandMask.FADE_WIDTH / 2;
        assertNotEquals(SpawnIslandMask.maskAt(ring, 0), SpawnIslandMask.maskAt(0, ring), EPS);
    }

    @Test
    void coastlineDiffersBetweenWorldSeeds() {
        // The same ring, two worlds: the seeded coastline phase must produce different bays.
        long seedA = 0x9E3779B97F4A7C15L;
        long seedB = 0xBF58476D1CE4E5B9L;
        int differences = 0;
        for (int deg = 0; deg < 360; deg += 5) {
            double rad = Math.toRadians(deg);
            int x = (int) Math.round(Math.cos(rad) * SpawnIslandMask.FULL_RADIUS);
            int z = (int) Math.round(Math.sin(rad) * SpawnIslandMask.FULL_RADIUS);
            if (Math.abs(SpawnIslandMask.maskAt(seedA, x, z) - SpawnIslandMask.maskAt(seedB, x, z)) > 1.0e-6D) {
                differences++;
            }
        }
        assertTrue(differences > 0, "two world seeds produced identical coastlines");
    }

    @Test
    void dunesOnlyRideTheOuterPlateauAndNeverStack() {
        // The organic flat core never shrinks below FLAT_RADIUS * 0.75.
        int guaranteedFlat = (int) (SpawnIslandMask.FLAT_RADIUS * 0.75D);
        for (long seed : SEEDS) {
            for (int x = -96; x <= 96; x += 5) {
                for (int z = -96; z <= 96; z += 5) {
                    double mask = SpawnIslandMask.maskAt(seed, x, z);
                    int dune = SpawnIslandMask.duneLift(seed, x, z, mask);
                    assertTrue(dune == 0 || dune == 1, "dune lift out of range");
                    if (mask < 1.0D) {
                        assertEquals(0, dune, "dune on the beach flank at (" + x + ", " + z + ")");
                    }
                    if ((double) x * x + (double) z * z <= (double) guaranteedFlat * guaranteedFlat) {
                        assertEquals(0, dune, "dune inside the flat building core at ("
                                + x + ", " + z + ")");
                    }
                }
            }
        }
        // Some outer-plateau ground must actually grow dunes, or the relief field is dead.
        int duneColumns = 0;
        for (int x = SpawnIslandMask.FLAT_RADIUS + 6; x <= SpawnIslandMask.FULL_RADIUS; x += 2) {
            for (int z = SpawnIslandMask.FLAT_RADIUS + 6; z <= SpawnIslandMask.FULL_RADIUS; z += 2) {
                duneColumns += SpawnIslandMask.duneLift(42L, x, z, 1.0D);
            }
        }
        assertTrue(duneColumns > 0, "no dunes anywhere on the outer plateau");
    }

    @Test
    void sandPatchesSpeckleThePlateauWithoutCoveringIt() {
        // Both grass and sand must occur on the fully emerged ground: the plateau is a
        // speckled grassland, not a sand disc and not a pure grass ring.
        int sand = 0;
        int grass = 0;
        for (int x = -SAFE_PLATEAU_RADIUS; x <= SAFE_PLATEAU_RADIUS; x += 3) {
            for (int z = -SAFE_PLATEAU_RADIUS; z <= SAFE_PLATEAU_RADIUS; z += 3) {
                if (SpawnIslandMask.sandPatchAt(42L, x, z)) {
                    sand++;
                } else {
                    grass++;
                }
            }
        }
        assertTrue(sand > 0, "no sand patches on the plateau");
        assertTrue(grass > 0, "no grass between the sand patches");
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
        // The blend runs on the squared mask, so the mid-flank (where the beach steepens into
        // the waterline) can legally swing ~100 * 2.16 / 100... bounded here at 3.0 blocks per
        // block including integer-sampling wobble of the coastline direction. This test guards
        // against tearing (multi-block jumps); relaxing slopes beyond 1.0 is CliffGuard's
        // contract on the guarded grid.
        for (long seed : SEEDS) {
            for (int deg = 0; deg < 180; deg += 22) {
                double rad = Math.toRadians(deg);
                double ux = Math.cos(rad);
                double uz = Math.sin(rad);
                double previous = SpawnIslandMask.blendFloor(RAW_FLOOR, SpawnIslandMask.maskAt(seed, 0, 0));
                for (int d = 1; d <= (int) Math.ceil(SpawnIslandMask.MAX_FADE_RADIUS); d++) {
                    int x = (int) Math.round(ux * d);
                    int z = (int) Math.round(uz * d);
                    double floor = SpawnIslandMask.blendFloor(RAW_FLOOR, SpawnIslandMask.maskAt(seed, x, z));
                    assertTrue(Math.abs(floor - previous) <= 3.0D,
                            "cliff in the blended island flank on the " + deg + " deg ray at d=" + d
                                    + ", seed " + seed);
                    previous = floor;
                }
            }
        }
    }
}
