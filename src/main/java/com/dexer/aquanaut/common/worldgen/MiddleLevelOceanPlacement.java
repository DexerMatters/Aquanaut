package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class MiddleLevelOceanPlacement {
    private static final int REGION_WEIGHT = 2;
    private static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "middle_level_ocean");
    private static final float HOLDER_ANCHOR_PARAMETER = 2.0F;
    private static final float HOLDER_ANCHOR_OFFSET = 1.0F;
    // Start the lower sea directly beneath a thin coral-forest ceiling.
    private static final int LAYER_START_BLOCK_Y = 28;
    private static final int SURFACE_SAMPLE_BLOCK_Y = 64;
    // Keep the vertical stack available anywhere vanilla treats the column as an ocean.
    private static final Set<ResourceLocation> VANILLA_OCEANS = Set.of(
            minecraft("deep_ocean"),
            minecraft("deep_cold_ocean"),
            minecraft("deep_lukewarm_ocean"),
            minecraft("deep_frozen_ocean"));

    private MiddleLevelOceanPlacement() {
    }

    public static int regionWeight() {
        return REGION_WEIGHT;
    }

    public static ResourceLocation location() {
        return LOCATION;
    }

    public static float holderAnchorParameter() {
        return HOLDER_ANCHOR_PARAMETER;
    }

    public static float holderAnchorOffset() {
        return HOLDER_ANCHOR_OFFSET;
    }

    public static int layerStartBlockY() {
        return LAYER_START_BLOCK_Y;
    }

    public static int layerStartQuartY() {
        return toQuartY(LAYER_START_BLOCK_Y);
    }

    public static int surfaceSampleQuartY() {
        return toQuartY(SURFACE_SAMPLE_BLOCK_Y);
    }

    public static boolean isMiddleLayerQuartY(int quartY) {
        return quartY <= layerStartQuartY();
    }

    public static boolean isVanillaOcean(ResourceLocation biomeLocation) {
        return VANILLA_OCEANS.contains(biomeLocation);
    }

    public static boolean isVanillaDeepOcean(ResourceLocation biomeLocation) {
        return isVanillaOcean(biomeLocation);
    }

    private static int toQuartY(int blockY) {
        return blockY >> 2;
    }

    private static ResourceLocation minecraft(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }
}
