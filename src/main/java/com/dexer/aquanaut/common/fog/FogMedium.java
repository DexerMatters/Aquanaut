package com.dexer.aquanaut.common.fog;

/**
 * The medium the camera is sitting in. Vanilla only recognises water, lava and powder snow
 * ({@code Camera#getFluidInCamera}), so a custom liquid such as sulfuric acid reports no
 * medium at all; the fog authority decides the medium itself and this enum is its vocabulary.
 *
 * <p>Ordered by priority: a camera inside acid inside a water biome is in acid.</p>
 */
public enum FogMedium {
    /** Open water: the biome's water fog colour and its visibility profile. */
    WATER,
    /** Sulfuric acid (硫酸): its own acrid colour, drawn in tighter than water. */
    ACID,
    /** Air, or any medium the authority does not claim: no fog of its own. */
    AIR;

    /** Whether this medium replaces the world's atmosphere while the camera is inside it. */
    public boolean submerged() {
        return this != AIR;
    }
}