package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;

public final class JellyJunglePlacement {
    private static final int REGION_WEIGHT = 2;
    private static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "jelly_jungle");
    private static final float HOLDER_ANCHOR_PARAMETER = 3.0F;
    private static final float HOLDER_ANCHOR_OFFSET = 1.5F;
    private static final int LAYER_START_BLOCK_Y = 36;

    private JellyJunglePlacement() {
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
        return LAYER_START_BLOCK_Y >> 2;
    }

    public static boolean isJellyJungleQuartY(int quartY) {
        return quartY <= layerStartQuartY() && quartY > MiddleLevelOceanPlacement.layerStartQuartY();
    }

    public static boolean isVanillaOcean(ResourceLocation biomeLocation) {
        return MiddleLevelOceanPlacement.isVanillaOcean(biomeLocation);
    }
}
