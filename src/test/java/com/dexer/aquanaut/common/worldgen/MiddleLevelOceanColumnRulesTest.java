package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

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
    void coralForestAndJellyJungleSplitTheThinTransitionIntoLargePatches() {
        MiddleLevelOceanColumnRules.TargetBiome patchBiome = MiddleLevelOceanColumnRules.targetBiome(
                minecraft("deep_ocean"), 16, 0, 9, 0);
        assertTrue(patchBiome == MiddleLevelOceanColumnRules.TargetBiome.CORAL_FOREST
                        || patchBiome == MiddleLevelOceanColumnRules.TargetBiome.JELLY_JUNGLE,
                "the transition band should route to either coral forest or jelly jungle");
        assertEquals(patchBiome,
                MiddleLevelOceanColumnRules.targetBiome(minecraft("deep_ocean"), 16, 2, 9, 2));
        assertEquals(patchBiome,
                MiddleLevelOceanColumnRules.targetBiome(minecraft("deep_ocean"), 16, 1, 8, 1));

        EnumSet<MiddleLevelOceanColumnRules.TargetBiome> seen = EnumSet.noneOf(
                MiddleLevelOceanColumnRules.TargetBiome.class);
        for (int quartX = -96; quartX <= 96; quartX += 24) {
            for (int quartZ = -96; quartZ <= 96; quartZ += 24) {
                seen.add(MiddleLevelOceanColumnRules.targetBiome(minecraft("deep_ocean"), 16, quartX, 9, quartZ));
            }
        }
        assertTrue(seen.contains(MiddleLevelOceanColumnRules.TargetBiome.CORAL_FOREST),
                "large-scale sampling should still produce coral forest patches");
        assertTrue(seen.contains(MiddleLevelOceanColumnRules.TargetBiome.JELLY_JUNGLE),
                "large-scale sampling should still produce jelly jungle patches");

        assertEquals(MiddleLevelOceanColumnRules.TargetBiome.MIDDLE_LEVEL_OCEAN,
                MiddleLevelOceanColumnRules.targetBiome(minecraft("deep_ocean"), 16, 0, 7, 0));
        assertEquals(MiddleLevelOceanColumnRules.TargetBiome.NONE,
                MiddleLevelOceanColumnRules.targetBiome(minecraft("deep_ocean"), 16, 0, 10, 0));
        assertEquals(MiddleLevelOceanColumnRules.TargetBiome.NONE,
                MiddleLevelOceanColumnRules.targetBiome(minecraft("deep_ocean"), 15, 0, 9, 0));
        assertEquals(MiddleLevelOceanColumnRules.TargetBiome.NONE,
                MiddleLevelOceanColumnRules.targetBiome(minecraft("ocean"), 16, 0, 9, 0));
    }

    private static ResourceLocation minecraft(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }
}
