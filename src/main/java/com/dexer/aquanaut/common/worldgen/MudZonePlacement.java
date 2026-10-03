package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;

/**
 * Placement identity of the Mud Zone (淤泥区), the deep sediment district of the middle
 * sea. Like the other districts it lives in the overworld biome source at its own climate
 * anchor, while the ocean layer stack decides where it actually grows.
 */
public final class MudZonePlacement {
    private static final int REGION_WEIGHT = 2;
    private static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "mud_zone");
    private static final float HOLDER_ANCHOR_PARAMETER = 3.75F;
    private static final float HOLDER_ANCHOR_OFFSET = 1.875F;

    private MudZonePlacement() {
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
