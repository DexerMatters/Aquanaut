package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;

/**
 * Placement constants for Brimstone Caldera, the volcanic middle-sea biome: fields of
 * giant sulfur-shrouded volcanoes, hot springs, fumaroles and sulfuric acid lakes.
 */
public final class BrimstoneCalderaPlacement {
    private static final int REGION_WEIGHT = 2;
    private static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "brimstone_caldera");
    private static final float HOLDER_ANCHOR_PARAMETER = 5.25F;
    private static final float HOLDER_ANCHOR_OFFSET = 2.6F;

    private BrimstoneCalderaPlacement() {
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
}
