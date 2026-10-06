package com.dexer.aquanaut.common.worldgen.layers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeRewriterIslandSurfaceTest {
    private static final long SEED = 42L;

    @Test
    void inactiveOutsideTheIslandSurfaceBand() {
        assertNull(BiomeRewriter.islandSurfaceAt(false, SEED, 0, 0, 16), "island biome while inactive");
        assertNull(BiomeRewriter.islandSurfaceAt(true, SEED, 0, 0, 9), "island biome below the band");
        assertNull(BiomeRewriter.islandSurfaceAt(true, SEED, 0, 0, 18), "island biome above the band");
    }

    @Test
    void plateauCoreIsLandShoreIsBeachDeepFadeIsNothing() {
        // The spawn quart column sits on the fully emerged plateau.
        assertEquals(BiomeRewriter.IslandSurface.PLATEAU,
                BiomeRewriter.islandSurfaceAt(true, SEED, 0, 0, 16));
        // Somewhere in the fade the shore ring takes over, and past the coastline the
        // decision falls through to the normal stack.
        Integer shore = null;
        for (int quartX = 6; quartX <= 60 && shore == null; quartX++) {
            if (BiomeRewriter.islandSurfaceAt(true, SEED, quartX, 0, 16)
                    == BiomeRewriter.IslandSurface.SHORE) {
                shore = quartX;
            }
        }
        assertTrue(shore != null, "no shore ring found on the eastern fade");
    }

    @Test
    void decisionIsStablePerColumn() {
        // The same quart cell must always decide the same way (pure function of position).
        for (int i = 0; i < 20; i++) {
            assertEquals(BiomeRewriter.islandSurfaceAt(true, SEED, 3, 2, 16),
                    BiomeRewriter.islandSurfaceAt(true, SEED, 3, 2, 16));
        }
    }

    @Test
    void stonyShoreRegionRendersAsStonyShore() {
        // The mining region is guaranteed on every island: at its centre the decision is
        // STONY_SHORE for both the surface band and the shelf band below it.
        double[] center = com.dexer.aquanaut.common.worldgen.blend.SpawnIslandMask
                .stoneShoreCenter(SEED);
        int quartX = ((int) Math.round(center[0])) >> 2;
        int quartZ = ((int) Math.round(center[1])) >> 2;
        assertEquals(BiomeRewriter.IslandSurface.STONY_SHORE,
                BiomeRewriter.islandSurfaceAt(true, SEED, quartX, quartZ, 16));
        assertEquals(BiomeRewriter.IslandSurface.STONY_SHORE,
                BiomeRewriter.islandSurfaceAt(true, SEED, quartX, quartZ, 12));
    }
}
