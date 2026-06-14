package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JellyJunglePlacementTest {
    @Test
    void biomeIdAndHiddenHolderAnchorStayStable() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("aquanaut", "jelly_jungle"),
                JellyJunglePlacement.location(), "biome id");
        assertEquals(2, JellyJunglePlacement.regionWeight(), "region weight");
        assertEquals(3.0F, JellyJunglePlacement.holderAnchorParameter(), 0.0001F, "hidden holder anchor");
        assertEquals(1.5F, JellyJunglePlacement.holderAnchorOffset(), 0.0001F, "hidden holder offset");
    }

    @Test
    void jellyJungleOccupiesTheSameThinTransitionBandAsCoralForest() {
        assertEquals(36, JellyJunglePlacement.layerStartBlockY(), "jelly jungle layer start block y");
        assertEquals(9, JellyJunglePlacement.layerStartQuartY(), "jelly jungle layer start quart y");

        assertTrue(JellyJunglePlacement.isJellyJungleQuartY(9), "start quart is inside the layer");
        assertTrue(JellyJunglePlacement.isJellyJungleQuartY(8), "the transition should stay thin but continuous");
        assertFalse(JellyJunglePlacement.isJellyJungleQuartY(7), "lower quart remains the middle ocean band");
        assertFalse(JellyJunglePlacement.isJellyJungleQuartY(10), "upper quart remains the ocean band");
    }

    @Test
    void deepOceanColumnsQualifyForTheJellyJungleRewrite() {
        assertTrue(JellyJunglePlacement.isVanillaOcean(minecraft("deep_ocean")));
        assertTrue(JellyJunglePlacement.isVanillaOcean(minecraft("deep_cold_ocean")));
        assertTrue(JellyJunglePlacement.isVanillaOcean(minecraft("deep_lukewarm_ocean")));
        assertTrue(JellyJunglePlacement.isVanillaOcean(minecraft("deep_frozen_ocean")));
        assertFalse(JellyJunglePlacement.isVanillaOcean(minecraft("ocean")));
    }

    private static ResourceLocation minecraft(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }
}
