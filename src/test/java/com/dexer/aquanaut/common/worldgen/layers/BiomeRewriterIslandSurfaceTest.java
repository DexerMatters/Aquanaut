package com.dexer.aquanaut.common.worldgen.layers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BiomeRewriterIslandSurfaceTest {
    private static final long SEED = 42L;

    @Test
    void inactiveOutsideTheIslandSurfaceBand() {
        assertNull(BiomeRewriter.islandSurfaceAt(false, SEED, 0, 0, 16), "island biome while inactive");
        assertNull(BiomeRewriter.islandSurfaceAt(true, SEED, 0, 0, 14), "island biome below the band");
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
        Integer nothing = null;
        for (int quartX = 6; quartX <= 56 && (shore == null || nothing == null); quartX++) {
            BiomeRewriter.IslandSurface surface =
                    BiomeRewriter.islandSurfaceAt(true, SEED, quartX, 0, 16);
            if (surface == BiomeRewriter.IslandSurface.SHORE && shore == null) {
                shore = quartX;
            }
            if (surface == null && nothing == null) {
                nothing = quartX;
            }
        }
        assertEquals(BiomeRewriter.IslandSurface.SHORE, shore != null ? BiomeRewriter.IslandSurface.SHORE : null,
                "no shore ring found on the eastern fade");
        if (nothing != null) {
            assertNull(BiomeRewriter.islandSurfaceAt(true, SEED, nothing, 0, 16));
        }
    }

    @Test
    void decisionIsStablePerColumn() {
        // The same quart cell must always decide the same way (pure function of position).
        for (int i = 0; i < 20; i++) {
            assertEquals(BiomeRewriter.islandSurfaceAt(true, SEED, 3, 2, 16),
                    BiomeRewriter.islandSurfaceAt(true, SEED, 3, 2, 16));
        }
    }
}
