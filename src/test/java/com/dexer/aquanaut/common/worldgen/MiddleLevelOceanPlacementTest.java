package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MiddleLevelOceanPlacementTest {

    @Test
    void biomeIdAndHiddenHolderAnchorStayStable() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("aquanaut", "middle_level_ocean"),
                MiddleLevelOceanPlacement.location(), "biome id");
        assertEquals(2, MiddleLevelOceanPlacement.regionWeight(), "region weight");
        assertEquals(2.0F, MiddleLevelOceanPlacement.holderAnchorParameter(), 0.0001F, "hidden holder anchor");
        assertEquals(1.0F, MiddleLevelOceanPlacement.holderAnchorOffset(), 0.0001F, "hidden holder offset");
    }

    @Test
    void verticalLayerIsABroadLowerOceanUnderThinCoralForest() {
        assertEquals(28, MiddleLevelOceanPlacement.layerStartBlockY(), "middle-ocean layer start block y");
        assertEquals(7, MiddleLevelOceanPlacement.layerStartQuartY(), "middle-ocean layer start quart y");
        assertEquals(16, MiddleLevelOceanPlacement.surfaceSampleQuartY(), "surface biome sample quart y");

        assertTrue(MiddleLevelOceanPlacement.isMiddleLayerQuartY(7), "start quart is inside the layer");
        assertTrue(MiddleLevelOceanPlacement.isMiddleLayerQuartY(2), "lower quart positions stay inside the layer");
        assertTrue(MiddleLevelOceanPlacement.isMiddleLayerQuartY(-16),
                "the middle ocean should keep extending far below the coral forest");
        assertFalse(MiddleLevelOceanPlacement.isMiddleLayerQuartY(8), "upper quart remains the coral forest band");
    }

    @Test
    void verticalLayerTargetsOnlyDeepOceanVariants() {
        assertTrue(MiddleLevelOceanPlacement.isVanillaOcean(minecraft("deep_ocean")));
        assertTrue(MiddleLevelOceanPlacement.isVanillaOcean(minecraft("deep_cold_ocean")));
        assertTrue(MiddleLevelOceanPlacement.isVanillaOcean(minecraft("deep_lukewarm_ocean")));
        assertTrue(MiddleLevelOceanPlacement.isVanillaOcean(minecraft("deep_frozen_ocean")));

        assertFalse(MiddleLevelOceanPlacement.isVanillaOcean(minecraft("ocean")));
        assertFalse(MiddleLevelOceanPlacement.isVanillaOcean(minecraft("warm_ocean")));
        assertFalse(MiddleLevelOceanPlacement.isVanillaOcean(minecraft("cold_ocean")));
        assertFalse(MiddleLevelOceanPlacement.isVanillaOcean(minecraft("plains")));
        assertFalse(MiddleLevelOceanPlacement.isVanillaOcean(MiddleLevelOceanPlacement.location()));
    }

    @Test
    void deepOceanColumnsQualifyForTheVerticalRewrite() {
        assertTrue(MiddleLevelOceanColumnRules.supportsQuartCell(minecraft("deep_ocean"), 16));
        assertTrue(MiddleLevelOceanColumnRules.supportsQuartCell(minecraft("deep_cold_ocean"), 16));
        assertTrue(MiddleLevelOceanColumnRules.supportsQuartCell(minecraft("deep_lukewarm_ocean"), 16));
        assertTrue(MiddleLevelOceanColumnRules.supportsQuartCell(minecraft("deep_frozen_ocean"), 16));
        assertFalse(MiddleLevelOceanColumnRules.supportsQuartCell(minecraft("ocean"), 16));
        assertFalse(MiddleLevelOceanColumnRules.supportsQuartCell(minecraft("deep_ocean"), 15));
        assertFalse(MiddleLevelOceanColumnRules.supportsQuartCell(minecraft("plains"), 16));
    }

    private static ResourceLocation minecraft(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }
}
