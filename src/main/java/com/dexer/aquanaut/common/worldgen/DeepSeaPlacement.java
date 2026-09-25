package com.dexer.aquanaut.common.worldgen;

import net.minecraft.resources.ResourceLocation;

/**
 * The abyssal deep sea below the reef that roofs it off from the middle sea. For now a
 * plain placeholder biome: the remaining world depth below the reef is one quiet,
 * featureless body of water until real abyssal biomes are designed for it.
 */
public final class DeepSeaPlacement {
    private static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "deep_sea");
    private static final float HOLDER_ANCHOR_PARAMETER = 4.0F;
    private static final float HOLDER_ANCHOR_OFFSET = 2.0F;

    private DeepSeaPlacement() {
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
