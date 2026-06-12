package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CoralForestPlacementTest {

    @Test
    void biomeIdAndHiddenHolderAnchorStayStable() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("aquanaut", "coral_forest"),
                CoralForestPlacement.location(), "biome id");
        assertEquals(2, CoralForestPlacement.regionWeight(), "region weight");
        assertEquals(2.5F, CoralForestPlacement.holderAnchorParameter(), 0.0001F, "hidden holder anchor");
        assertEquals(1.25F, CoralForestPlacement.holderAnchorOffset(), 0.0001F, "hidden holder offset");
    }

    @Test
    void coralForestIsAThinTransitionBandBetweenTwoOceans() {
        assertEquals(36, CoralForestPlacement.layerStartBlockY(), "coral forest layer start block y");
        assertEquals(9, CoralForestPlacement.layerStartQuartY(), "coral forest layer start quart y");

        assertTrue(CoralForestPlacement.isCoralForestQuartY(9), "start quart is inside the layer");
        assertTrue(CoralForestPlacement.isCoralForestQuartY(8), "the transition should stay thin but continuous");
        assertFalse(CoralForestPlacement.isCoralForestQuartY(7), "lower quart remains the middle ocean band");
        assertFalse(CoralForestPlacement.isCoralForestQuartY(10), "upper quart remains the ocean band");
    }

    @Test
    void deepOceanColumnsQualifyForTheCoralForestRewrite() {
        assertTrue(CoralForestLayering.shouldReplaceBiome(minecraft("deep_ocean"), 9));
        assertTrue(CoralForestLayering.shouldReplaceBiome(minecraft("deep_ocean"), 8));
        assertTrue(CoralForestLayering.shouldReplaceBiome(minecraft("deep_cold_ocean"), 9));
        assertTrue(CoralForestLayering.shouldReplaceBiome(minecraft("deep_lukewarm_ocean"), 9));
        assertTrue(CoralForestLayering.shouldReplaceBiome(minecraft("deep_frozen_ocean"), 9));
        assertFalse(CoralForestLayering.shouldReplaceBiome(minecraft("ocean"), 9));
        assertFalse(CoralForestLayering.shouldReplaceBiome(minecraft("warm_ocean"), 9));
        assertFalse(CoralForestLayering.shouldReplaceBiome(minecraft("deep_ocean"), 7));
        assertFalse(CoralForestLayering.shouldReplaceBiome(minecraft("deep_ocean"), 10));
        assertFalse(CoralForestLayering.shouldReplaceBiome(minecraft("plains"), 9));
    }

    private static ResourceLocation minecraft(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }
}
