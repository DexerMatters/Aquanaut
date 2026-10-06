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
    /** Comfortably inside the tightest bay of any seed (MIN_PLATEAU_RADIUS = 85). */
    private static final int SAFE_PLATEAU_RADIUS = 32;
    /** Comfortably inside the organic flat building core (never below FLAT_RADIUS * 0.75). */
    private static final int GUARANTEED_FLAT_RADIUS = 18;
    /** Land threshold of the squared blend: floor reaches sea level at mask 0.9644 (raw -30). */
    private static final double LAND_MASK = 0.9644D;
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
    void coastIsMeanderingNotACircle() {
        // The maintainer's bar: the coastline must not read as a circle. Walk 72 rays and
        // measure where each one crosses the waterline; the spread has to be far beyond the
        // couple of blocks a circular coast would produce.
        int minLand = Integer.MAX_VALUE;
        int maxLand = 0;
        for (int deg = 0; deg < 360; deg += 5) {
            double rad = Math.toRadians(deg);
            double ux = Math.cos(rad);
            double uz = Math.sin(rad);
            int land = 0;
            for (int d = (int) SpawnIslandMask.MIN_PLATEAU_RADIUS;
                    d <= (int) SpawnIslandMask.COAST_MEAN_RADIUS + SpawnIslandMask.COAST_AMPLITUDE + 10; d++) {
                int x = (int) Math.round(ux * d);
                int z = (int) Math.round(uz * d);
                if (SpawnIslandMask.maskAt(42L, x, z) >= LAND_MASK) {
                    land = d;
                }
            }
            assertTrue(land > 0, "no land on the " + deg + " deg ray");
            minLand = Math.min(minLand, land);
            maxLand = Math.max(maxLand, land);
        }
        assertTrue(maxLand - minLand >= 30,
                "coastline spread only " + (maxLand - minLand) + " blocks - still a circle");
    }

    @Test
    void coastlineIsContinuousAroundTheOriginAndDiffersPerSeed() {
        // Walking a ring at the mean coast radius must not jump: the warped multi-octave
        // field is continuous, so the coastline never tears at a chunk border or the ±pi seam.
        int ring = SpawnIslandMask.COAST_MEAN_RADIUS;
        double previous = SpawnIslandMask.maskAt(ring, 0);
        for (int i = 1; i <= 180; i++) {
            double rad = 2.0D * Math.PI * i / 180;
            int x = (int) Math.round(Math.cos(rad) * ring);
            int z = (int) Math.round(Math.sin(rad) * ring);
            double mask = SpawnIslandMask.maskAt(x, z);
            assertTrue(Math.abs(mask - previous) <= 0.08D, "coastline jumped at sample " + i);
            previous = mask;
        }
        // Two world seeds meander differently.
        assertNotEquals(SpawnIslandMask.maskAt(42L, ring, 0),
                SpawnIslandMask.maskAt(-77L, ring, 0), EPS);
    }

    @Test
    void interiorLiftRollsTheOuterPlateauButNeverTheFlatCore() {
        for (long seed : SEEDS) {
            for (int x = -96; x <= 96; x += 4) {
                for (int z = -96; z <= 96; z += 4) {
                    double mask = SpawnIslandMask.maskAt(seed, x, z);
                    double lift = SpawnIslandMask.interiorLift(seed, x, z, mask);
                    assertTrue(lift >= -3.0D - EPS && lift <= 3.0D + EPS,
                            "interior lift out of range at (" + x + ", " + z + ")");
                    if (mask < 1.0D) {
                        assertEquals(0.0D, lift, EPS, "relief on the beach flank");
                    }
                    double dist = Math.sqrt((double) x * x + (double) z * z);
                    if (dist <= GUARANTEED_FLAT_RADIUS) {
                        assertEquals(0.0D, lift, EPS, "relief inside the flat core");
                    }
                }
            }
        }
        // The outer plateau must actually roll, or the island stays a featureless disc.
        double maxLift = 0.0D;
        double minLift = 0.0D;
        for (int x = -SpawnIslandMask.COAST_MEAN_RADIUS; x <= SpawnIslandMask.COAST_MEAN_RADIUS; x += 3) {
            for (int z = -SpawnIslandMask.COAST_MEAN_RADIUS; z <= SpawnIslandMask.COAST_MEAN_RADIUS; z += 3) {
                double lift = SpawnIslandMask.interiorLift(42L, x, z, 1.0D);
                maxLift = Math.max(maxLift, lift);
                minLift = Math.min(minLift, lift);
            }
        }
        assertTrue(maxLift - minLift >= 1.5D, "plateau relief is dead flat");
    }

    @Test
    void dunesOnlyRideTheOuterPlateauAndNeverStack() {
        for (long seed : SEEDS) {
            for (int x = -96; x <= 96; x += 5) {
                for (int z = -96; z <= 96; z += 5) {
                    double mask = SpawnIslandMask.maskAt(seed, x, z);
                    int dune = SpawnIslandMask.duneLift(seed, x, z, mask);
                    assertTrue(dune == 0 || dune == 1, "dune lift out of range");
                    if (mask < 1.0D) {
                        assertEquals(0, dune, "dune on the beach flank at (" + x + ", " + z + ")");
                    }
                    double dist = Math.sqrt((double) x * x + (double) z * z);
                    if (dist <= GUARANTEED_FLAT_RADIUS) {
                        assertEquals(0, dune, "dune inside the flat building core at ("
                                + x + ", " + z + ")");
                    }
                }
            }
        }
        // Some outer-plateau ground must actually grow dunes, or the relief field is dead.
        int duneColumns = 0;
        for (int x = SpawnIslandMask.FLAT_RADIUS + 6; x <= SpawnIslandMask.COAST_MEAN_RADIUS; x += 2) {
            for (int z = SpawnIslandMask.FLAT_RADIUS + 6; z <= SpawnIslandMask.COAST_MEAN_RADIUS; z += 2) {
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
    void stonyShoreZoneIsAlwaysPresentWithItsLavaPond() {
        // Every island carries the mining region: around the zone centre the weight crosses
        // 0.5 and the lava pond exists inside it, for every world seed.
        for (long seed : SEEDS) {
            double[] center = SpawnIslandMask.stoneShoreCenter(seed);
            int cx = (int) Math.round(center[0]);
            int cz = (int) Math.round(center[1]);
            assertTrue(SpawnIslandMask.stoneShoreWeight(seed, cx, cz) >= 0.5D,
                    "stony shore missing at its centre, seed " + seed);
            assertTrue(SpawnIslandMask.stoneShoreWeight(seed, cx, cz) <= 1.0D, "weight out of range");
            assertTrue(SpawnIslandMask.maskAt(seed, cx, cz) >= 1.0D,
                    "stony shore centre off the plateau, seed " + seed);
            boolean lava = false;
            for (int x = cx - 16; x <= cx + 16 && !lava; x += 2) {
                for (int z = cz - 16; z <= cz + 16 && !lava; z += 2) {
                    lava = SpawnIslandMask.lavaPoolAt(seed, x, z);
                }
            }
            assertTrue(lava, "no lava pond inside the stony shore, seed " + seed);
        }
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
        // The squared blend's steepest mid-flank swings ~2.2 blocks per block, and the coast
        // field gradient plus integer ray sampling add a little on top. This test guards
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
                    assertTrue(Math.abs(floor - previous) <= 3.5D,
                            "cliff in the blended island flank on the " + deg + " deg ray at d=" + d
                                    + ", seed " + seed);
                    previous = floor;
                }
            }
        }
    }
}
