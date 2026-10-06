package com.dexer.aquanaut.common.worldgen.blend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnIslandCavesTest {
    private static final long[] SEEDS = {0L, 1L, 42L, -99L, 0x9E3779B97F4A7C15L};

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
    void cavesExistInsideTheCoreButStaySparse() {
        // A vertical slab through the plateau core: some cave, not a Swiss cheese.
        for (long seed : SEEDS) {
            int cells = 0;
            int total = 0;
            for (int x = -40; x <= 40; x += 2) {
                for (int y = SpawnIslandCaves.CAVE_BOTTOM_Y; y <= 56; y += 2) {
                    for (int z = -40; z <= 40; z += 2) {
                        total++;
                        if (SpawnIslandCaves.isCave(seed, x, y, z)) {
                            cells++;
                        }
                    }
                }
            }
            double fraction = cells / (double) total;
            assertTrue(fraction > 0.005D, "no caves at all, seed " + seed);
            assertTrue(fraction < 0.40D, "island core is swiss cheese, seed " + seed);
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
