package com.dexer.aquanaut.common.fog;

/**
 * The fog authority's arithmetic, kept free of Minecraft so every ramp is unit-testable.
 *
 * <p>Two things are decided here. How deep the viewer is ({@link #ramp}), a single [0, 1]
 * figure that both the world's fog state and the pack-proof veil are driven from; and what
 * that figure does to the eye's reach ({@link #nearPlane}, {@link #farPlane}) and to the
 * veil's opacity ({@link #veilAlpha}).</p>
 */
public final class FogMath {
    /** Where the fog has closed to at the bottom of the abyss. */
    public static final float DEEPEST_NEAR_PLANE = -4.0F;
    public static final float DEEPEST_FAR_PLANE = 8.0F;
    /**
     * Night vision cannot lift the pressure part of the darkness past this: it is a
     * reprieve from the deep, not a way to ignore it.
     */
    public static final float NIGHT_VISION_CAP = 0.9F;
    /**
     * The veil never covers more of the screen than this, so a shader pack that *also*
     * honours the fog state we hand it can never be pushed into double darkness.
     */
    public static final float MAX_VEIL_ALPHA = 0.55F;
    /** The colour cast a medium puts on the view before any depth is accounted for. */
    public static final float VEIL_BASE_CAST = 0.45F;
    /** Brightness the vanilla lightmap is scaled to at the very bottom of the abyss. */
    public static final float LIGHTMAP_ABYSS_FLOOR = 0.10F;
    /**
     * The colour the abyss pulls every medium toward: near-black with the faintest cold blue
     * left in it, so the deepest water reads as depth rather than as a broken screen.
     */
    public static final int ABYSS_RGB = 0x03050D;

    private FogMath() {
    }

    /**
     * How deep the viewer is, in [0, 1]: the water pressure they are under, or their
     * drowning darkness once that is deeper. Night vision caps the pressure part only —
     * drowning still takes the whole view.
     */
    public static float ramp(double pressure, double drowningDarkness, boolean nightVision) {
        float deep = clamp01((float) pressure);
        if (nightVision) {
            deep = Math.min(deep, NIGHT_VISION_CAP);
        }
        return Math.max(deep, clamp01((float) drowningDarkness));
    }

    /** The medium's own near plane, pulled in toward the abyss plane by {@code ramp}. */
    public static float nearPlane(FogVisibility visibility, float ramp) {
        return lerp(clamp01(ramp), visibility.nearPlane(), DEEPEST_NEAR_PLANE);
    }

    /** The medium's own far plane, pulled in toward the abyss plane by {@code ramp}. */
    public static float farPlane(FogVisibility visibility, float ramp) {
        return lerp(clamp01(ramp), visibility.farPlane(), DEEPEST_FAR_PLANE);
    }

    /**
     * Opacity of the pack-proof veil: the medium's cast, scaled by how deep the viewer is
     * and by the player's configured strength, and bounded by {@link #MAX_VEIL_ALPHA}.
     */
    public static float veilAlpha(FogVisibility visibility, float ramp, float configuredStrength) {
        float cast = clamp01(visibility.castStrength()) * clamp01(configuredStrength);
        if (cast <= 0.0F) {
            return 0.0F;
        }
        float depth = clamp01(ramp);
        float shaped = VEIL_BASE_CAST + (1.0F - VEIL_BASE_CAST) * depth;
        return Math.min(MAX_VEIL_ALPHA, cast * shaped);
    }

    /** The vanilla lightmap's brightness at this depth: 1 untouched, floored at the abyss. */
    public static float lightmapScale(float ramp) {
        return lightmapScale(ramp, LIGHTMAP_ABYSS_FLOOR);
    }

    /**
     * The vanilla lightmap's brightness at this depth, dimming toward {@code floor} rather
     * than to black — a floor the player sets, so nobody is left unable to see their own hand.
     */
    public static float lightmapScale(float ramp, float floor) {
        return lerp(clamp01(ramp), 1.0F, clamp01(floor));
    }

    /**
     * How a medium's colour looks at this depth. Both consumers of the fog authority — the
     * world's own fog state and the veil drawn over the frame — take their colour from here,
     * so the two are tinted identically by construction.
     */
    public static int fogRgb(int baseRgb, float ramp) {
        return mixRgb(baseRgb, ABYSS_RGB, ramp);
    }

    /**
     * A weighted average of several colours: what a boundary between two oceans looks like.
     * Weights need not be normalised, and an all-zero weight set yields {@code fallback}.
     */
    public static int blendRgb(int[] colours, double[] weights, int fallback) {
        if (colours.length != weights.length || colours.length == 0) {
            return fallback;
        }
        double total = 0.0D;
        double red = 0.0D;
        double green = 0.0D;
        double blue = 0.0D;
        for (int i = 0; i < colours.length; i++) {
            double weight = Math.max(0.0D, weights[i]);
            total += weight;
            red += ((colours[i] >> 16) & 0xFF) * weight;
            green += ((colours[i] >> 8) & 0xFF) * weight;
            blue += (colours[i] & 0xFF) * weight;
        }
        if (total <= 0.0D) {
            return fallback;
        }
        int r = (int) Math.round(red / total);
        int g = (int) Math.round(green / total);
        int b = (int) Math.round(blue / total);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Frame-rate independent easing: move {@code current} a fraction of the way to
     * {@code target} such that the remaining distance decays with time constant {@code tau}
     * seconds, whatever the frame rate.
     */
    public static float ease(float current, float target, float seconds, float tau) {
        if (seconds <= 0.0F) {
            return current;
        }
        if (tau <= 0.0F) {
            return target;
        }
        float delta = 1.0F - (float) Math.exp(-seconds / tau);
        return lerp(clamp01(delta), current, target);
    }

    /** Blend two packed 0xRRGGBB colours. */
    public static int mixRgb(int from, int to, float delta) {
        float t = clamp01(delta);
        int fromRed = (from >> 16) & 0xFF;
        int fromGreen = (from >> 8) & 0xFF;
        int fromBlue = from & 0xFF;
        int toRed = (to >> 16) & 0xFF;
        int toGreen = (to >> 8) & 0xFF;
        int toBlue = to & 0xFF;
        int red = Math.round(fromRed + (toRed - fromRed) * t);
        int green = Math.round(fromGreen + (toGreen - fromGreen) * t);
        int blue = Math.round(fromBlue + (toBlue - fromBlue) * t);
        return (red << 16) | (green << 8) | blue;
    }

    public static float lerp(float delta, float from, float to) {
        return from + (to - from) * delta;
    }

    public static float clamp01(float value) {
        return value < 0.0F ? 0.0F : Math.min(value, 1.0F);
    }
}