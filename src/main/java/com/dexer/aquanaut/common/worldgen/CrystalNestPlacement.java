package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;

/**
 * Placement identity of the Crystal Nest (水晶巢), the geode-lattice biome of the middle
 * sea. Like the other hidden holders it lives in the overworld biome source at its own
 * climate anchor, while the ocean layer stack decides where it actually grows.
 */
public final class CrystalNestPlacement {
    private static final int REGION_WEIGHT = 2;
    private static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "crystal_nest");
    private static final float HOLDER_ANCHOR_PARAMETER = 4.5F;
    private static final float HOLDER_ANCHOR_OFFSET = 2.25F;

    private CrystalNestPlacement() {
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

    public static boolean isVanillaOcean(ResourceLocation biomeLocation) {
        return MiddleLevelOceanPlacement.isVanillaOcean(biomeLocation);
    }
}
