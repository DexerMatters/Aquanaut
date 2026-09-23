package com.dexer.aquanaut.common.worldgen.layers;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class OceanLayerStackTest {

    @Test
    void defaultStackHasThreeLayersAndDeepOceanParents() {
        OceanLayerStack stack = OceanLayerStacks.defaultStack();
        assertEquals(3, stack.layers().size());
        assertTrue(stack.isParentBiome(minecraft("deep_ocean")));
        assertFalse(stack.isParentBiome(minecraft("ocean")));
        assertTrue(stack.supportsQuartCell(minecraft("deep_ocean"), 16));
        assertFalse(stack.supportsQuartCell(minecraft("deep_ocean"), 15));
    }

    @Test
    void rewriteRangeCoversReefAndMiddleSeaOnly() {
        OceanLayerStack stack = OceanLayerStacks.defaultStack();
        assertEquals(-64, stack.minRewriteBlockY());
        assertEquals(39, stack.maxRewriteBlockY());
    }

    @Test
    void dominantLayerIsReefInBandAndMiddleBelow() {
        OceanLayerStack stack = OceanLayerStacks.defaultStack();
        assertEquals("aquanaut:reef_ceiling", stack.dominantLayerAtBlockY(36).id().toString());
        assertEquals("aquanaut:middle_sea", stack.dominantLayerAtBlockY(20).id().toString());
        assertEquals("aquanaut:surface_ocean", stack.dominantLayerAtBlockY(64).id().toString());
    }

    @Test
    void middleSeaBandIsLargerAndMeetsTheReefCeilingFloor() {
        OceanLayerStack stack = OceanLayerStacks.defaultStack();
        OceanLayer reef = stack.layers().get(1);
        OceanLayer middle = stack.layers().get(2);
        // The middle sea is the larger band and now reaches up to the reef ceiling's own floor
        // (was 31), so the deep-ocean biome range no longer overhangs it.
        assertEquals(35, reef.band().minY());
        assertEquals(reef.band().minY() - 1, middle.band().maxY(),
                "middle sea should reach the reef ceiling floor");
        assertTrue(middle.band().maxY() - middle.band().minY()
                        > reef.band().maxY() - reef.band().minY(),
                "middle sea should span more blocks than the reef ceiling");
    }

    @Test
    void reefMixProducesBothBiomesAcrossLargeSampling() {
        BiomeMix mix = OceanLayer.reefCeiling().mix();
        Set<ResourceLocation> seen = new HashSet<>();
        for (int x = -200; x <= 200; x += 25) {
            for (int z = -200; z <= 200; z += 25) {
                seen.add(mix.dominantBiomeAt(x, z));
            }
        }
        assertTrue(seen.contains(ResourceLocation.fromNamespaceAndPath("aquanaut", "coral_forest")));
        assertTrue(seen.contains(ResourceLocation.fromNamespaceAndPath("aquanaut", "jelly_jungle")));
    }

    @Test
    void reefMixWeightsAreSoftNearBorders() {
        BiomeMix mix = OceanLayer.reefCeiling().mix();
        int mixed = 0;
        for (int x = -128; x <= 128; x++) {
            double[] weights = mix.weightsAt(x, 0);
            assertEquals(1.0, weights[0] + weights[1], 1e-6);
            if (weights[0] > 0.15 && weights[1] > 0.15) {
                mixed++;
            }
        }
        assertTrue(mixed > 0, "soft horizontal blend should produce mixed borders");
    }

    @Test
    void jsonRoundTripPreservesDefaultStack() {
        OceanLayerStack stack = OceanLayerStacks.defaultStack();
        String json = OceanLayerStackJson.toJson(stack);
        OceanLayerStack parsed = OceanLayerStackJson.fromJson(json);
        assertEquals(stack.id(), parsed.id());
        assertEquals(stack.layers().size(), parsed.layers().size());
        assertEquals(stack.layers().get(1).mix().entries().get(0).biome(),
                parsed.layers().get(1).mix().entries().get(0).biome());
        assertEquals(stack.minOpenWaterColumns(), parsed.minOpenWaterColumns());
    }

    private static ResourceLocation minecraft(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }
}
