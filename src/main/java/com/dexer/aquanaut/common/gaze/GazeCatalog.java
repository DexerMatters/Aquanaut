package com.dexer.aquanaut.common.gaze;

import com.dexer.aquanaut.Aquanaut;
import net.minecraft.resources.ResourceLocation;

public final class GazeCatalog {
    public static final ResourceLocation PENETRATION = id("penetration");
    public static final ResourceLocation RECALLING = id("recalling");
    public static final ResourceLocation SHADOW = id("shadow");
    public static final ResourceLocation FLOATING = id("floating");

    private GazeCatalog() {
    }

    public static int maxLevel(ResourceLocation id) {
        if (PENETRATION.equals(id) || SHADOW.equals(id) || FLOATING.equals(id)) {
            return 3;
        }
        if (RECALLING.equals(id)) {
            return 1;
        }
        return Integer.MAX_VALUE;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, path);
    }
}
