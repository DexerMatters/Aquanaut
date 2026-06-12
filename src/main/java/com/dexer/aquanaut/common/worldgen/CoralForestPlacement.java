package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;

public final class CoralForestPlacement {
    private static final int REGION_WEIGHT = 2;
    private static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "coral_forest");
    private static final float HOLDER_ANCHOR_PARAMETER = 2.5F;
    private static final float HOLDER_ANCHOR_OFFSET = 1.25F;
    private static final int LAYER_START_BLOCK_Y = 36;
    private static final int SURFACE_SAMPLE_BLOCK_Y = 64;

    private CoralForestPlacement() {
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

    public static boolean isCoralForestQuartY(int quartY) {
        return quartY <= layerStartQuartY() && quartY > MiddleLevelOceanPlacement.layerStartQuartY();
    }

    public static boolean isVanillaOcean(ResourceLocation biomeLocation) {
        return MiddleLevelOceanPlacement.isVanillaOcean(biomeLocation);
    }

    public static boolean isVanillaDeepOcean(ResourceLocation biomeLocation) {
        return isVanillaOcean(biomeLocation);
    }

    private static int toQuartY(int blockY) {
        return blockY >> 2;
    }
}
