package com.dexer.aquanaut.common.fog;

/**
 * A medium the world has no colour for, so it must bring its own: sulfuric acid is a real
 * fluid with no vanilla {@code FogType}, and nothing in the biome effects describes it.
 *
 * @param rgb        packed 0xRRGGBB colour the acid washes the view with
 * @param visibility how far the eye reaches through it
 */
public record FogMediumProfile(int rgb, FogVisibility visibility) {

    public float red() {
        return ((rgb >> 16) & 0xFF) / 255.0F;
    }

    public float green() {
        return ((rgb >> 8) & 0xFF) / 255.0F;
    }

    public float blue() {
        return (rgb & 0xFF) / 255.0F;
    }
}