package com.dexer.aquanaut.common.fog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fog authority's ramps: how deep the viewer is, what that does to the eye's reach, and
 * how far the pack-proof veil is allowed to go.
 */
public final class FogMathTest {

    @Test
    void rampTakesTheDeeperOfPressureAndDrowning() {
        assertEquals(0.20F, FogMath.ramp(0.20D, 0.0D, false), 1e-6F);
        assertEquals(0.80F, FogMath.ramp(0.20D, 0.80D, false), 1e-6F,
                "drowning darkness wins when it is deeper than the water pressure");
        assertEquals(1.0F, FogMath.ramp(2.0D, 1.5D, false), 1e-6F, "ramp never exceeds 1");
        assertEquals(0.0F, FogMath.ramp(-1.0D, -1.0D, false), 1e-6F, "ramp never drops below 0");
    }

    @Test
    void nightVisionCapsPressureButNotDrowning() {
        assertEquals(FogMath.NIGHT_VISION_CAP, FogMath.ramp(1.0D, 0.0D, true), 1e-6F,
                "night vision is a reprieve from the pressure");
        assertEquals(1.0F, FogMath.ramp(0.5D, 1.0D, true), 1e-6F,
                "but drowning still takes the whole view");
        assertTrue(FogMath.ramp(0.5D, 0.0D, true) < FogMath.NIGHT_VISION_CAP + 1e-6F);
    }

    @Test
    void planesCloseTowardTheAbyssAsTheRampRises() {
        FogVisibility visibility = new FogVisibility(-8.0F, 128.0F, 0.4F);
        assertEquals(-8.0F, FogMath.nearPlane(visibility, 0.0F), 1e-6F);
        assertEquals(128.0F, FogMath.farPlane(visibility, 0.0F), 1e-6F);
        assertEquals(FogMath.DEEPEST_NEAR_PLANE, FogMath.nearPlane(visibility, 1.0F), 1e-6F);
        assertEquals(FogMath.DEEPEST_FAR_PLANE, FogMath.farPlane(visibility, 1.0F), 1e-6F);

        float previousNear = Float.NEGATIVE_INFINITY;
        float previousFar = Float.POSITIVE_INFINITY;
        for (int step = 0; step <= 10; step++) {
            float ramp = step / 10.0F;
            float near = FogMath.nearPlane(visibility, ramp);
            float far = FogMath.farPlane(visibility, ramp);
            assertTrue(near >= previousNear, "the near plane only ever moves inward");
            assertTrue(far <= previousFar, "the far plane only ever closes in");
            assertTrue(far > near, "the fog never turns inside out");
            previousNear = near;
            previousFar = far;
        }
    }

    @Test
    void veilIsBoundedAndNeverInvertsWithDepth() {
        FogVisibility visibility = new FogVisibility(-4.0F, 96.0F, 0.8F);
        assertEquals(0.0F, FogMath.veilAlpha(visibility, 0.5F, 0.0F), 1e-6F,
                "a player who turns the veil off gets none of it");
        assertEquals(0.0F, FogMath.veilAlpha(visibility.withCast(0.0F), 0.5F, 1.0F), 1e-6F,
                "a medium with no cast never veils");

        float shallow = FogMath.veilAlpha(visibility, 0.0F, 1.0F);
        float deep = FogMath.veilAlpha(visibility, 1.0F, 1.0F);
        assertTrue(shallow > 0.0F, "being inside a medium tints the view even at no depth");
        assertTrue(deep > shallow, "and deepens with depth");
        assertTrue(deep <= FogMath.MAX_VEIL_ALPHA + 1e-6F,
                "the veil is capped so a cooperative pack is never double-darkened");
        assertEquals(FogMath.MAX_VEIL_ALPHA, FogMath.veilAlpha(visibility, 1.0F, 4.0F), 1e-6F,
                "out-of-range strength settings are clamped, not obeyed");
    }

    @Test
    void lightmapDarkensTowardItsFloor() {
        assertEquals(1.0F, FogMath.lightmapScale(0.0F), 1e-6F);
        assertEquals(FogMath.LIGHTMAP_ABYSS_FLOOR, FogMath.lightmapScale(1.0F), 1e-6F);
        assertTrue(FogMath.lightmapScale(0.5F) < 1.0F);
        assertTrue(FogMath.lightmapScale(0.5F) > FogMath.LIGHTMAP_ABYSS_FLOOR);
    }

    @Test
    void mixRgbBlendsEachChannel() {
        assertEquals(0x000000, FogMath.mixRgb(0x000000, 0xFFFFFF, 0.0F));
        assertEquals(0xFFFFFF, FogMath.mixRgb(0x000000, 0xFFFFFF, 1.0F));
        assertEquals(0x808080, FogMath.mixRgb(0x000000, 0xFFFFFF, 0.5F));
        assertEquals(0x336699, FogMath.mixRgb(0x336699, 0x336699, 0.7F));
        assertEquals(0x000000, FogMath.mixRgb(0x000000, 0xFFFFFF, -1.0F), "t is clamped");
    }

    @Test
    void blendRgbWeightsColoursWithoutNormalisingThem() {
        int[] rgbs = { 0x000000, 0xFFFFFF };
        assertEquals(0x000000, FogMath.blendRgb(rgbs, new double[] { 1.0D, 0.0D }, 0x123456));
        assertEquals(0xFFFFFF, FogMath.blendRgb(rgbs, new double[] { 0.0D, 1.0D }, 0x123456));
        assertEquals(0x808080, FogMath.blendRgb(rgbs, new double[] { 1.0D, 1.0D }, 0x123456));
        assertEquals(0x808080, FogMath.blendRgb(rgbs, new double[] { 5.0D, 5.0D }, 0x123456),
                "only the ratio of the weights matters");
        assertEquals(0xAAAAAA, FogMath.blendRgb(rgbs, new double[] { 1.0D, 2.0D }, 0x123456),
                "the heavier sample pulls the blend");
        assertEquals(0x123456, FogMath.blendRgb(rgbs, new double[] { 0.0D, 0.0D }, 0x123456),
                "an empty weighting falls back rather than dividing by zero");
        assertEquals(0x123456, FogMath.blendRgb(new int[0], new double[0], 0x123456));
        assertEquals(0x123456, FogMath.blendRgb(rgbs, new double[] { 1.0D }, 0x123456),
                "mismatched lengths fall back");
    }

    @Test
    void easeIsFrameRateIndependentAndSettlesOnTheTarget() {
        // Two half-steps at a quarter of the time constant must land where one full step does.
        float oneStep = FogMath.ease(0.0F, 1.0F, 0.1F, 0.1F);
        float half = FogMath.ease(0.0F, 1.0F, 0.05F, 0.1F);
        float twoSteps = FogMath.ease(half, 1.0F, 0.05F, 0.1F);
        assertEquals(FogMath.ease(0.0F, 1.0F, 0.1F, 0.1F), oneStep, 1e-6F);
        assertEquals(oneStep, twoSteps, 1e-5F, "the same elapsed time must give the same result");

        assertTrue(FogMath.ease(0.0F, 1.0F, 1.0F, 0.2F) > 0.98F, "it does converge");
        assertEquals(1.0F, FogMath.ease(0.0F, 1.0F, 10.0F, 0.2F), 1e-3F);
        assertEquals(0.0F, FogMath.ease(0.0F, 1.0F, 0.0F, 0.2F), 1e-6F,
                "a zero-length frame does not move anything");
        assertEquals(1.0F, FogMath.ease(0.5F, 1.0F, 0.1F, 0.0F), 1e-6F,
                "a zero time constant snaps to the target");
    }

    @Test
    void visibilityClampsToSurvivableValues() {
        FogVisibility wild = new FogVisibility(-900.0F, 9000.0F, 4.0F);
        assertEquals(FogVisibility.MIN_NEAR_PLANE, wild.nearPlane(), 1e-6F);
        assertEquals(FogVisibility.MAX_FAR_PLANE, wild.farPlane(), 1e-6F);
        assertEquals(1.0F, wild.castStrength(), 1e-6F);
        assertEquals(-8.0F, new FogVisibility(-8.0F, 128.0F, 0.4F).nearPlane(), 1e-6F,
                "vanilla's own water fog starts behind the eye and must stay there");

        FogVisibility pinched = new FogVisibility(0.0F, -50.0F, -3.0F);
        assertEquals(FogVisibility.MIN_FAR_PLANE, pinched.farPlane(), 1e-6F,
                "the eye never closes to nothing");
        assertEquals(0.0F, pinched.castStrength(), 1e-6F);
    }
}