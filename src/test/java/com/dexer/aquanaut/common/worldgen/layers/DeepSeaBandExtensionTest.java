package com.dexer.aquanaut.common.worldgen.layers;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Extending the world's Y range changes exactly one band: the abyss — the deepest layer — grows
 * downward until it is as tall as the band above it (the middle sea). The surface ocean, the reef
 * ceiling and the middle sea keep their authored position and height, and the area below the abyss
 * band stays unclaimed for future abyssal content.
 */
public final class DeepSeaBandExtensionTest {

    private static final int REFERENCE = -64;
    private static final int ABYSSAL = -512;
    /** Layer order: surface ocean, reef ceiling, middle sea, deep sea. */
    private static final int SURFACE = 0;
    private static final int REEF = 1;
    private static final int MIDDLE = 2;
    private static final int DEEP = 3;

    private static DepthBand band(OceanLayerStack stack, int index) {
        return stack.layers().get(index).band();
    }

    private static int height(DepthBand band) {
        return band.maxY() - band.minY() + 1;
    }

    @Test
    void referenceFloorIsTheAuthoredLayout() {
        OceanLayerStack stack = OceanLayerStacks.activeFor(REFERENCE);

        assertSame(OceanLayerStacks.defaultStack(), stack,
                "a vanilla-height world must resolve to the authored stack itself");
        assertEquals(40, band(stack, SURFACE).minY());
        assertEquals(320, band(stack, SURFACE).maxY());
        assertEquals(35, band(stack, REEF).minY());
        assertEquals(39, band(stack, REEF).maxY());
        assertEquals(-42, band(stack, MIDDLE).minY());
        assertEquals(34, band(stack, MIDDLE).maxY());
        assertEquals(-64, band(stack, DEEP).minY());
        assertEquals(-38, band(stack, DEEP).maxY());
    }

    @Test
    void onlyTheAbyssBandGrowsWithTheWorldFloor() {
        OceanLayerStack reference = OceanLayerStacks.activeFor(REFERENCE);
        OceanLayerStack stack = OceanLayerStacks.activeFor(ABYSSAL);

        assertNotSame(reference, stack);
        assertEquals(band(reference, SURFACE), band(stack, SURFACE), "surface ocean is untouched");
        assertEquals(band(reference, REEF), band(stack, REEF), "reef ceiling is untouched");
        assertEquals(band(reference, MIDDLE), band(stack, MIDDLE), "middle sea is untouched");
        assertEquals(band(reference, DEEP).maxY(), band(stack, DEEP).maxY(),
                "the abyss keeps its authored top");
        assertEquals(-114, band(stack, DEEP).minY(), "the abyss grows downward to the new floor");
    }

    @Test
    void abyssBandEndsUpAsTallAsTheMiddleSea() {
        OceanLayerStack stack = OceanLayerStacks.activeFor(ABYSSAL);
        int middle = height(band(stack, MIDDLE));
        int abyss = height(band(stack, DEEP));

        assertEquals(77, middle, "the middle sea keeps its authored height");
        assertEquals(middle, abyss, "the abyss must be as tall as the middle sea");
    }

    @Test
    void areaBelowTheAbyssBandStaysUnclaimed() {
        OceanLayerStack stack = OceanLayerStacks.activeFor(ABYSSAL);
        int unclaimedFrom = band(stack, DEEP).minY() - 1;

        assertEquals(unclaimedFrom, -115);
        for (int y = ABYSSAL; y <= unclaimedFrom; y++) {
            for (OceanLayer layer : stack.layers()) {
                assertEquals(0.0D, layer.band().weightAtBlockY(y),
                        layer.id() + " must not claim the reserved depth at y=" + y);
            }
        }
        // The biome rewriter only touches cells a band claims, so it stops right above the
        // reserved area instead of stretching the abyss over it.
        assertEquals(unclaimedFrom + 1, stack.minRewriteBlockY());
        assertEquals(39, stack.maxRewriteBlockY());
    }

    @Test
    void bandClaimsFollowTheExtendedAbyss() {
        OceanLayerStack stack = OceanLayerStacks.activeFor(ABYSSAL);

        assertEquals("aquanaut:deep_sea", stack.dominantLayerAtBlockY(ABYSSAL + 400).id().toString());
        assertEquals("aquanaut:deep_sea", stack.dominantLayerAtBlockY(-100).id().toString());
        assertEquals("aquanaut:middle_sea", stack.dominantLayerAtBlockY(-40).id().toString());
        assertEquals("aquanaut:middle_sea", stack.dominantLayerAtBlockY(20).id().toString());
        assertEquals("aquanaut:reef_ceiling", stack.dominantLayerAtBlockY(36).id().toString());
        assertEquals("aquanaut:surface_ocean", stack.dominantLayerAtBlockY(64).id().toString());
    }

    @Test
    void intermediateFloorsClampTheAbyssToTheWorld() {
        // Just deep enough to fit the matching band: the extension is fully realised.
        assertEquals(-114, band(OceanLayerStacks.activeFor(-128), DEEP).minY());
        // Shallower than the matching height: the world floor clamps it.
        assertEquals(-100, band(OceanLayerStacks.activeFor(-100), DEEP).minY());
        // At and above the authored floor nothing moves at all.
        assertSame(OceanLayerStacks.defaultStack(), OceanLayerStacks.activeFor(-48));
        assertSame(OceanLayerStacks.defaultStack(), OceanLayerStacks.activeFor(-64));
    }

    @Test
    void supportRulesAreUnchangedByTheExtension() {
        OceanLayerStack stack = OceanLayerStacks.activeFor(ABYSSAL);

        assertTrue(stack.supportsQuartCell(ResourceLocation.withDefaultNamespace("deep_ocean"), 16));
        assertFalse(stack.supportsQuartCell(ResourceLocation.withDefaultNamespace("ocean"), 16));
        assertTrue(stack.isParentBiome(ResourceLocation.withDefaultNamespace("deep_frozen_ocean")));
    }
}
