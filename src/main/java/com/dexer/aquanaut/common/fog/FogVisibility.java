package com.dexer.aquanaut.common.fog;

/**
 * How far the eye reaches through one medium, and how strongly that medium tints the view.
 *
 * <p>Geometry only — the colour is never stored here. While the camera is submerged the
 * authoritative colour comes from the world's own fog state (the biome's water fog colour,
 * modified by the abyss ramp), so a biome's colour is defined in exactly one place: its
 * biome JSON. Only media the world has no colour for (sulfuric acid) carry one themselves,
 * in {@link FogMediumProfile}.</p>
 *
 * @param nearPlane     distance the fog starts at, in blocks (negative starts behind the eye)
 * @param farPlane      distance the fog closes at, in blocks
 * @param castStrength  how strongly the medium tints the screen, 0 (not at all) to 1
 */
public record FogVisibility(float nearPlane, float farPlane, float castStrength) {
    /** The murkiest a medium may be drawn: the eye never closes tighter than this. */
    public static final float MIN_FAR_PLANE = 4.0F;
    /** The clearest a medium may be drawn: beyond it a medium stops reading as a medium. */
    public static final float MAX_FAR_PLANE = 192.0F;
    /**
     * How far behind the eye the fog may start. Vanilla's own water fog starts at -8, so the
     * range has to reach past that; anything further back is a typo, not an intention.
     */
    public static final float MIN_NEAR_PLANE = -32.0F;

    public FogVisibility {
        nearPlane = clamp(nearPlane, MIN_NEAR_PLANE, MAX_FAR_PLANE);
        farPlane = clamp(farPlane, MIN_FAR_PLANE, MAX_FAR_PLANE);
        castStrength = clamp(castStrength, 0.0F, 1.0F);
    }

    /** A copy with the given planes, keeping the cast. */
    public FogVisibility withPlanes(float near, float far) {
        return new FogVisibility(near, far, castStrength);
    }

    /** A copy with the given cast strength, keeping the planes. */
    public FogVisibility withCast(float cast) {
        return new FogVisibility(nearPlane, farPlane, cast);
    }

    static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }
}