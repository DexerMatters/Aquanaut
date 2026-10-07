package com.dexer.aquanaut.common.worldgen.blend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnIslandCavesTest {
    private static final long[] SEEDS = {0L, 1L, 42L, -99L, 0x9E3779B97F4A7C15L};

    @Test
    void cavesExistInsideTheCoreButStaySparse() {
        // A vertical slab through the plateau core: real caves in every depth band, but
        // nowhere near a Swiss cheese. Measured band: 1.3% - 6.8% of the envelope.
        for (long seed : SEEDS) {
            int cells = 0;
            int total = 0;
            double[] band = new double[3];
            double[] bandTotal = new double[3];
            for (int x = -40; x <= 40; x += 2) {
                for (int y = SpawnIslandCaves.CAVE_BOTTOM_Y; y <= SpawnIslandCaves.CAVE_TOP_Y; y += 2) {
                    for (int z = -40; z <= 40; z += 2) {
                        total++;
                        boolean cave = SpawnIslandCaves.isCave(seed, x, y, z);
                        if (cave) {
                            cells++;
                            band[y < -20 ? 0 : (y < 20 ? 1 : 2)]++;
                        }
                        bandTotal[y < -20 ? 0 : (y < 20 ? 1 : 2)]++;
                    }
                }
            }
            double fraction = cells / (double) total;
            assertTrue(fraction > 0.005D, "no caves at all, seed " + seed);
            assertTrue(fraction < 0.12D, "island core is swiss cheese, seed " + seed);
            for (int b = 0; b < 3; b++) {
                assertTrue(band[b] / bandTotal[b] > 0.004D,
                        "depth band " + b + " is caveless, seed " + seed);
            }
        }
    }

    @Test
    void voidsNeverTouchTheEnvelopePlanes() {
        // Tunnels pinch to a point and caverns fade to impossible at the cave floor and the
        // protected-surface ceiling: no void may end in a flat, layer-like plane.
        for (long seed : SEEDS) {
            for (int x = -48; x <= 48; x += 3) {
                for (int z = -48; z <= 48; z += 3) {
                    assertTrue(!SpawnIslandCaves.isCave(seed, x, SpawnIslandCaves.CAVE_BOTTOM_Y, z),
                            "void cut flat on the cave floor plane");
                    assertTrue(!SpawnIslandCaves.isCave(seed, x, SpawnIslandCaves.CAVE_TOP_Y, z),
                            "void cut flat on the surface-protection plane");
                }
            }
        }
    }

    @Test
    void caveBodiesStayCompactInsteadOfLayered() {
        // The old sheet-based fields intersected into lens-shaped geological void layers:
        // standing inside one, a flat hollow stretched on and on. True 3D pockets and tubes
        // keep vertical runs short (measured max 54 across the probe seeds); a run through
        // most of the envelope height would mean a stratified void sheet again.
        for (long seed : SEEDS) {
            int maxRun = 0;
            for (int x = -40; x <= 40; x += 2) {
                for (int z = -40; z <= 40; z += 2) {
                    int run = 0;
                    for (int y = SpawnIslandCaves.CAVE_BOTTOM_Y; y <= SpawnIslandCaves.CAVE_TOP_Y; y++) {
                        if (SpawnIslandCaves.isCave(seed, x, y, z)) {
                            run++;
                            maxRun = Math.max(maxRun, run);
                        } else {
                            run = 0;
                        }
                    }
                }
            }
            assertTrue(maxRun < 60, "cave void reads as a geological layer, seed " + seed
                    + ": vertical run " + maxRun);
        }
    }

    @Test
    void cavesAreDeterministic() {
        for (long seed : SEEDS) {
            for (int i = 0; i < 50; i++) {
                int x = 37 * i - 400;
                int y = SpawnIslandCaves.CAVE_BOTTOM_Y + 7 * i;
                int z = -23 * i + 300;
                assertEquals(SpawnIslandCaves.isCave(seed, x, y, z),
                        SpawnIslandCaves.isCave(seed, x, y, z),
                        "cave field unstable at (" + x + ", " + y + ", " + z + ")");
            }
        }
    }

    @Test
    void nothingBelowTheCaveFloor() {
        for (long seed : SEEDS) {
            for (int y = SpawnIslandCaves.CAVE_BOTTOM_Y - 1; y > SpawnIslandCaves.CAVE_BOTTOM_Y - 40; y -= 3) {
                assertTrue(!SpawnIslandCaves.isCave(seed, 12, y, -34),
                        "cave below the floor at y=" + y);
            }
        }
    }

    @Test
    void caveLayoutsDifferBetweenWorldSeeds() {
        int differences = 0;
        for (int x = -64; x <= 64; x += 3) {
            for (int y = SpawnIslandCaves.CAVE_BOTTOM_Y; y <= 56; y += 3) {
                for (int z = -64; z <= 64; z += 3) {
                    if (SpawnIslandCaves.isCave(42L, x, y, z) != SpawnIslandCaves.isCave(-77L, x, y, z)) {
                        differences++;
                    }
                }
            }
        }
        assertTrue(differences > 0, "two world seeds produced identical caves");
    }
}
