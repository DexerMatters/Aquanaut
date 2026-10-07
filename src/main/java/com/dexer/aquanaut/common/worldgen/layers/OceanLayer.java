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
                        // Distinct salts: with three entries the mix uses per-entry noise (argmax),
                        // so coral and jelly can no longer share one field without one shadowing
                        // the other.
                        MixEntry.patch(ResourceLocation.fromNamespaceAndPath("aquanaut", "jelly_jungle"),
                                0.5D, 32, 11, -7, 3L),
                        // The mud flats: broad sediment shelves sharing the reef band with the
                        // coral and jelly provinces, so the mud zone sits at the same depth.
                        MixEntry.patch(ResourceLocation.fromNamespaceAndPath("aquanaut", "mud_zone"),
                                0.5D, 40, -19, 9, 71L)
                ), 3.0D),
                ResourceLocation.fromNamespaceAndPath("aquanaut", "reef_cap"),
                true);
    }

    /**
     * Lower sea: Brine Mirror Gorge amphitheaters, the volcanic province of Brimstone
     * Caldera on its own noise channel, the Crystal Nest's skinned geode lattice on yet
     * another, and Middle-Level Ocean between them. The gorge stays the dominant
     * continuous floor biome; the caldera and the nest form large, coherent districts
     * where their channels win.
     */
    public static OceanLayer middleSea() {
        return new OceanLayer(
                ResourceLocation.fromNamespaceAndPath("aquanaut", "middle_sea"),
                new DepthBand(-42, 34, 2.0D, 0.0D),
                new BiomeMix(java.util.List.of(
                        MixEntry.patch(ResourceLocation.fromNamespaceAndPath("aquanaut", "middle_level_ocean"),
                                0.30D, 80, 19, -13, 7L),
                        MixEntry.patch(ResourceLocation.fromNamespaceAndPath("aquanaut", "brine_mirror_gorge"),
                                0.48D, 80, 19, -13, 7L),
                        MixEntry.patch(ResourceLocation.fromNamespaceAndPath("aquanaut", "brimstone_caldera"),
                                0.45D, 80, 19, -13, 23L),
                        // The Crystal Nest grows on its own channel so the geode lattice forms
                        // coherent districts and softens into its neighbors at the borders.
                        // District sizes settle near brine : crystal : caldera = 1 : 1.2 : 0.8.
                        MixEntry.patch(ResourceLocation.fromNamespaceAndPath("aquanaut", "crystal_nest"),
                                0.52D, 80, 31, -27, 41L)
                ), 4.0D),
                ResourceLocation.fromNamespaceAndPath("aquanaut", "middle_cavity"),
                true);
    }

    /**
     * The abyssal deep sea below the reef that roofs it off from the middle sea. One quiet
     * placeholder biome spans the remaining world depth until real abyssal biomes exist.
     * Its terrain module is the same middle-cavity module: the planner shapes both seas
     * and the reef between them from one column story.
     */
    public static OceanLayer deepSea() {
        return new OceanLayer(
                ResourceLocation.fromNamespaceAndPath("aquanaut", "deep_sea"),
                new DepthBand(-64, -38, 0.0D, 4.0D),
                new BiomeMix(java.util.List.of(
                        MixEntry.of(ResourceLocation.fromNamespaceAndPath("aquanaut", "deep_sea"))
                ), 0.0D),
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
