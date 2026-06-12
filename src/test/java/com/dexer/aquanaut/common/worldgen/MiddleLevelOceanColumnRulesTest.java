package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MiddleLevelOceanColumnRulesTest {
    @Test
    void onlyFullySubmergedDeepOceanQuartCellsKeepTheVerticalStack() {
        assertTrue(MiddleLevelOceanColumnRules.supportsQuartCell(minecraft("deep_ocean"), 16));
        assertTrue(MiddleLevelOceanColumnRules.supportsQuartCell(minecraft("deep_cold_ocean"), 16));

        assertFalse(MiddleLevelOceanColumnRules.supportsQuartCell(minecraft("deep_ocean"), 15));
        assertFalse(MiddleLevelOceanColumnRules.supportsQuartCell(minecraft("ocean"), 16));
        assertFalse(MiddleLevelOceanColumnRules.supportsQuartCell(minecraft("plains"), 16));
    }

    @Test
    void coralForestOnlyOccupiesTheThinTransitionOfValidDeepOceanCells() {
        assertEquals(MiddleLevelOceanColumnRules.TargetBiome.CORAL_FOREST,
                MiddleLevelOceanColumnRules.targetBiome(minecraft("deep_ocean"), 16, 9));
        assertEquals(MiddleLevelOceanColumnRules.TargetBiome.CORAL_FOREST,
                MiddleLevelOceanColumnRules.targetBiome(minecraft("deep_ocean"), 16, 8));
        assertEquals(MiddleLevelOceanColumnRules.TargetBiome.MIDDLE_LEVEL_OCEAN,
                MiddleLevelOceanColumnRules.targetBiome(minecraft("deep_ocean"), 16, 7));
        assertEquals(MiddleLevelOceanColumnRules.TargetBiome.NONE,
                MiddleLevelOceanColumnRules.targetBiome(minecraft("deep_ocean"), 16, 10));
        assertEquals(MiddleLevelOceanColumnRules.TargetBiome.NONE,
                MiddleLevelOceanColumnRules.targetBiome(minecraft("deep_ocean"), 15, 9));
        assertEquals(MiddleLevelOceanColumnRules.TargetBiome.NONE,
                MiddleLevelOceanColumnRules.targetBiome(minecraft("ocean"), 16, 9));
    }

    private static ResourceLocation minecraft(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }
}
