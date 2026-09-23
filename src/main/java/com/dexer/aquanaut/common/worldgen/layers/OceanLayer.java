package com.dexer.aquanaut.common.worldgen.layers;

import net.minecraft.resources.ResourceLocation;

/**
 * One vertical depth layer of the stereoscopic ocean stack.
 */
public record OceanLayer(ResourceLocation id,
                         DepthBand band,
                         BiomeMix mix,
                         ResourceLocation terrain,
                         boolean carve) {

    public static OceanLayer surface() {
        return new OceanLayer(
                ResourceLocation.fromNamespaceAndPath("aquanaut", "surface_ocean"),
                new DepthBand(40, 320, 0.0D, 0.0D),
                BiomeMix.inheritSurface(),
                ResourceLocation.withDefaultNamespace("none"),
                false);
    }

    public static OceanLayer reefCeiling() {
        return new OceanLayer(
                ResourceLocation.fromNamespaceAndPath("aquanaut", "reef_ceiling"),
                new DepthBand(35, 39, 2.0D, 2.0D),
                new BiomeMix(java.util.List.of(
                        MixEntry.patch(ResourceLocation.fromNamespaceAndPath("aquanaut", "coral_forest"),
                                0.5D, 32, 11, -7),
                        MixEntry.patch(ResourceLocation.fromNamespaceAndPath("aquanaut", "jelly_jungle"),
                                0.5D, 32, 11, -7)
                ), 3.0D),
                ResourceLocation.fromNamespaceAndPath("aquanaut", "reef_cap"),
                true);
    }

    /**
     * Lower sea: Brine Mirror Gorge amphitheaters, the volcanic province of Brimstone
     * Caldera on its own noise channel, and Middle-Level Ocean between them. The gorge
     * stays the dominant continuous floor biome; the caldera forms large, coherent
     * volcanic districts where its channel wins.
     */
    public static OceanLayer middleSea() {
        return new OceanLayer(
                ResourceLocation.fromNamespaceAndPath("aquanaut", "middle_sea"),
                new DepthBand(-64, 34, 2.0D, 0.0D),
                new BiomeMix(java.util.List.of(
                        MixEntry.patch(ResourceLocation.fromNamespaceAndPath("aquanaut", "middle_level_ocean"),
                                0.30D, 80, 19, -13, 7L),
                        MixEntry.patch(ResourceLocation.fromNamespaceAndPath("aquanaut", "brine_mirror_gorge"),
                                0.55D, 80, 19, -13, 7L),
                        MixEntry.patch(ResourceLocation.fromNamespaceAndPath("aquanaut", "brimstone_caldera"),
                                0.35D, 80, 19, -13, 23L)
                ), 4.0D),
                ResourceLocation.fromNamespaceAndPath("aquanaut", "middle_cavity"),
                true);
    }

    public boolean rewritesBiome() {
        if (mix.entries().size() == 1 && mix.entries().get(0).inheritSurface()) {
            return false;
        }
        return true;
    }
}
