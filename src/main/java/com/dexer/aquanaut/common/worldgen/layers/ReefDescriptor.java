package com.dexer.aquanaut.common.worldgen.layers;

import com.dexer.aquanaut.common.worldgen.BrimstoneCalderaPlacement;
import com.dexer.aquanaut.common.worldgen.BrineMirrorGorgePlacement;
import com.dexer.aquanaut.common.worldgen.CrystalNestPlacement;
import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanPlacement;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The continuous geological character one middle-sea district imparts to the reef slab
 * between the two seas. This replaces the old per-column {@code ReefKind} argmax: instead
 * of one district winning a column outright (and its neighbours ending on a 12-block
 * wall or a cut-off crack field), every district contributes its thickness, solidity,
 * crack affinity and rock family <em>proportionally to its interpolated weight</em>, and
 * the reef composes those contributions:
 *
 * <ul>
 *   <li><b>thickness</b> blends linearly, so the crystal nest's open reef pinches out
 *       over the whole weight transition instead of dropping off a cliff;</li>
 *   <li><b>solidity</b> drives the karst dissolution field — intermediate weights get
 *       irregular, growing dissolution windows rather than a hard OPEN/SOLID border;</li>
 *   <li><b>crack affinity</b> fades the brine fissures in and out, so they narrow and
 *       pinch shut across the district border instead of being severed by it;</li>
 *   <li><b>family</b> feeds the contact dither, interbedding volcanic apron rock with
 *       sedimentary strata and brine-flavoured shale/halite lenses through the contact
 *       zone.</li>
 * </ul>
 *
 * <p>Datapack districts can register their own descriptor; described entries are exactly
 * the entries that drive the reef composition.</p>
 */
public record ReefDescriptor(double baseThickness,
                             double thicknessAmplitude,
                             int thicknessCell,
                             long thicknessSeed,
                             double solidity,
                             double crackAffinity,
                             Family family) {

    /** Rock families of the reef slab; ordinal is the index into dither weight vectors. */
    public enum Family {
        /** Quiet middle sea: banded sedimentary strata. */
        SEDIMENTARY,
        /** Brimstone Caldera: the monolithic volcanic concrete apron. */
        VOLCANIC,
        /** Brine Mirror Gorge: varve shale with halite-crust lenses. */
        HALITE;

        public static final Family[] VALUES = values();
    }

    private static final Map<ResourceLocation, ReefDescriptor> REGISTRY = new ConcurrentHashMap<>();

    static {
        // Quiet middle-level ocean: solid strata, 9..15 blocks.
        REGISTRY.put(MiddleLevelOceanPlacement.location(),
                new ReefDescriptor(12.0D, 3.0D, 32, 0x5EEDFACE1L, 1.0D, 0.0D, Family.SEDIMENTARY));
        // Brine Mirror Gorge: the same strata, fissured by cracks that widen downward.
        REGISTRY.put(BrineMirrorGorgePlacement.location(),
                new ReefDescriptor(12.0D, 3.0D, 32, 0x5EEDFACE1L, 1.0D, 1.0D, Family.HALITE));
        // Brimstone Caldera: thick monolithic apron, 13..17 blocks.
        REGISTRY.put(BrimstoneCalderaPlacement.location(),
                new ReefDescriptor(15.0D, 2.0D, 48, 0x5EEDFACE1L, 1.0D, 0.0D, Family.VOLCANIC));
        // Crystal Nest: no floor at all — both seas run into one another.
        REGISTRY.put(CrystalNestPlacement.location(),
                new ReefDescriptor(0.0D, 0.0D, 32, 0x5EEDFACE1L, 0.0D, 0.0D, Family.SEDIMENTARY));
    }

    /** The descriptor registered for a district biome, or {@code null} when it does not drive the reef. */
    public static ReefDescriptor get(ResourceLocation biome) {
        return biome == null ? null : REGISTRY.get(biome);
    }

    public static void register(ResourceLocation biome, ReefDescriptor descriptor) {
        REGISTRY.put(biome, descriptor);
    }

    /** Fallback geology of undescribed ground: solid sedimentary strata. */
    public static ReefDescriptor defaultSolid() {
        return new ReefDescriptor(12.0D, 3.0D, 32, 0x5EEDFACE1L, 1.0D, 0.0D, Family.SEDIMENTARY);
    }

    /** Noise-modulated slab thickness of this family at a world column. */
    public double thicknessAt(int blockX, int blockZ) {
        if (thicknessAmplitude <= 0.0D) {
            return baseThickness;
        }
        return baseThickness
                + SoftMixNoise.valueNoise(blockX, blockZ, thicknessCell, thicknessSeed) * thicknessAmplitude;
    }
}
