package com.dexer.aquanaut.common.worldgen.layers;

import net.minecraft.resources.ResourceLocation;

/**
 * One candidate biome on a layer. When {@code inheritSurface} is true the stack keeps the
 * vanilla surface deep-ocean variant instead of rewriting to {@code biome}.
 */
public record MixEntry(ResourceLocation biome,
                       double weight,
                       int noiseScaleQuarts,
                       int noiseOffsetX,
                       int noiseOffsetZ,
                       long noiseSalt,
                       boolean inheritSurface) {

    public static MixEntry of(ResourceLocation biome) {
        return new MixEntry(biome, 1.0D, 32, 0, 0, 2L, false);
    }

    public static MixEntry surfaceInherited() {
        return new MixEntry(ResourceLocation.withDefaultNamespace("deep_ocean"),
                1.0D, 32, 0, 0, 2L, true);
    }

    public static MixEntry patch(ResourceLocation biome, double weight, int noiseScaleQuarts, int offsetX, int offsetZ) {
        return new MixEntry(biome, weight, noiseScaleQuarts, offsetX, offsetZ, 2L, false);
    }

    public static MixEntry patch(ResourceLocation biome, double weight, int noiseScaleQuarts, int offsetX, int offsetZ, long noiseSalt) {
        return new MixEntry(biome, weight, noiseScaleQuarts, offsetX, offsetZ, noiseSalt, false);
    }
}
