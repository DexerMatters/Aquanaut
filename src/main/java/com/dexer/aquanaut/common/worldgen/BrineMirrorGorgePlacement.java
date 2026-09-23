package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;

public final class BrineMirrorGorgePlacement {
    private static final int REGION_WEIGHT = 2;
    private static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "brine_mirror_gorge");
    private static final float HOLDER_ANCHOR_PARAMETER = 3.5F;
    private static final float HOLDER_ANCHOR_OFFSET = 1.75F;

    private BrineMirrorGorgePlacement() {
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
