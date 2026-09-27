package com.dexer.aquanaut.common.fog;

/**
 * The medium the camera is sitting in. The fog authority decides the medium itself and
 * this enum is its vocabulary.
 */
public enum FogMedium {
    /** Open water: the biome's water fog colour and its visibility profile. */
    WATER,
    /** Air, or any medium the authority does not claim: no fog of its own. */
    AIR;

    /** Whether this medium replaces the world's atmosphere while the camera is inside it. */
    public boolean submerged() {
        return this != AIR;
    }
}
