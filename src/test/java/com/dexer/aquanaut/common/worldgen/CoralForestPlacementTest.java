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
    void reefBandRoutesToEitherCoralForestOrJellyJungle() {
        MiddleLevelOceanColumnRules.TargetBiome band =
                MiddleLevelOceanColumnRules.targetBiome(minecraft("deep_ocean"), 16, 0, 9, 0);
        assertTrue(band == MiddleLevelOceanColumnRules.TargetBiome.CORAL_FOREST
                        || band == MiddleLevelOceanColumnRules.TargetBiome.JELLY_JUNGLE,
                "reef band should route to a custom reef biome");
        MiddleLevelOceanColumnRules.TargetBiome deep =
                MiddleLevelOceanColumnRules.targetBiome(minecraft("deep_ocean"), 16, 0, 7, 0);
        assertTrue(deep == MiddleLevelOceanColumnRules.TargetBiome.MIDDLE_LEVEL_OCEAN
                        || deep == MiddleLevelOceanColumnRules.TargetBiome.BRINE_MIRROR_GORGE,
                "middle-sea layer is MLO or brine mirror gorge");
        assertEquals(MiddleLevelOceanColumnRules.TargetBiome.NONE,
                MiddleLevelOceanColumnRules.targetBiome(minecraft("deep_ocean"), 16, 0, 10, 0));
    }

    private static ResourceLocation minecraft(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }
}
