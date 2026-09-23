package com.dexer.aquanaut.client.fog;

import com.dexer.aquanaut.common.PressureHelper;
import com.dexer.aquanaut.common.fog.FogMath;
import com.dexer.aquanaut.common.fog.FogMedium;
import com.dexer.aquanaut.common.fog.FogMediumProfile;
import com.dexer.aquanaut.common.fog.FogProfiles;
import com.dexer.aquanaut.common.fog.FogVisibility;
import com.dexer.aquanaut.core.TagRegistry;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;

/**
 * The one place that decides what the water looks like: which medium the camera is in, what
 * colour it should be, how far the eye reaches through it, and how hard the depth presses
 * against the screen.
 *
 * <h3>Colour has exactly one owner</h3>
 * While the camera is submerged the colour is resolved here — a blend of the oceans around the
 * camera (see {@link FogField}) — and handed to the world's fog state unchanged. A shader pack
 * reading Iris's {@code fogColor} uniform and the veil drawn over the frame both take it from
 * that one value, so they cannot disagree.
 *
 * <h3>Nothing changes in a single frame</h3>
 * Every quantity the authority reports is <em>eased</em> toward its target with a time
 * constant, so crossing an ocean boundary, sinking between the vertical layers, or breaking
 * the surface is a fade rather than a cut. The blending in {@link FogField} supplies the
 * spatial gradient; this easing covers what a gradient cannot — the step from one medium to
 * another, such as swimming into acid. {@code submersion} is the one weight that ties the
 * whole thing together: at 1 the camera is fully in the medium and its fog replaces the
 * world's, at 0 the world's own fog is untouched, and in between the two are mixed.
 *
 * <p>Every decision is about <em>a camera</em>, not about a player: the drone's feed renders
 * the world a second time through a camera of its own, and that camera keeps the water's own
 * look instead of inheriting the operator's pressure or drowning darkness. Callers exclude it
 * before asking (see {@code ClientFogEvents}), so nothing here has to know about the pod.</p>
 */
public final class ClientFogState {
    /** Seconds for the colour to close most of the gap to its target. */
    private static final float COLOUR_TAU = 0.35F;
    /** Seconds for the eye's reach to close in. */
    private static final float PLANE_TAU = 0.30F;
    /** Seconds for entering or leaving a medium, which also fades the veil and the tint. */
    private static final float SUBMERSION_TAU = 0.25F;
    /** Longest frame the easing will honour, so a stalled frame cannot jump the view. */
    private static final float MAX_STEP_SECONDS = 0.25F;

    /** What the camera is inside, and everything that follows from it. */
    public record Snapshot(FogMedium medium, boolean submerged, float submersion,
                           float red, float green, float blue,
                           float nearPlane, float farPlane,
                           float ramp, float veilAlpha) {

        /** The medium's colour, packed, before the abyss ramp is applied. */
        public int rgb() {
            int r = Math.round(FogMath.clamp01(red) * 255.0F);
            int g = Math.round(FogMath.clamp01(green) * 255.0F);
            int b = Math.round(FogMath.clamp01(blue) * 255.0F);
            return (r << 16) | (g << 8) | b;
        }

        /** The colour the view actually takes at this depth. */
        public int fogRgb() {
            return FogMath.fogRgb(rgb(), ramp);
        }

        /** Whether the fallback veil has anything at all to draw. */
        public boolean veiled() {
            return veilAlpha > 0.001F;
        }
    }

    private static final Snapshot AIR = new Snapshot(FogMedium.AIR, false, 0.0F,
            1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F);

    private static volatile Snapshot snapshot = AIR;
    /** The local player's drowning darkness, ticked by {@code ClientFogEvents}. */
    private static volatile float drowningDarkness = 0.0F;

    private static float easedRed = 1.0F;
    private static float easedGreen = 1.0F;
    private static float easedBlue = 1.0F;
    private static float easedNear;
    private static float easedFar;
    private static float easedVeil;
    private static float submersion;
    private static boolean seeded;
    /** The frame timer reading the eased state last advanced on. */
    private static float lastFrameTicks = Float.NaN;

    private ClientFogState() {
    }

    /** The last fog resolved for the operator's own camera. */
    public static Snapshot get() {
        return snapshot;
    }

    public static float drowningDarkness() {
        return drowningDarkness;
    }

    public static void setDrowningDarkness(float darkness) {
        drowningDarkness = FogMath.clamp01(darkness);
    }

    /**
     * Resolve the fog for the operator's own camera, once per frame.
     *
     * <p>The fog events fire more than once in a frame — the sky pass and the terrain pass each
     * set fog up — so the eased state advances on the first call and every later call in the
     * same frame is handed the same answer. Without that guard the easing would run at double
     * speed on a frame that happened to set fog up twice.</p>
     */
    public static Snapshot resolve(Camera camera, float red, float green, float blue) {
        float ticks = frameTicks();
        if (ticks == lastFrameTicks) {
            return snapshot;
        }
        lastFrameTicks = ticks;

        ClientLevel level = Minecraft.getInstance().level;
        FogMedium medium = mediumAt(camera);
        float ramp = rampFor(camera);
        float seconds = Math.min(MAX_STEP_SECONDS, ticks / 20.0F);

        if (medium.submerged() && level != null) {
            // Aim at the blended neighbourhood rather than at the one biome underfoot.
            FogField.Blend blend = FogField.sample(level, camera.getPosition());
            if (medium == FogMedium.ACID) {
                FogMediumProfile acid = FogProfiles.get().acid();
                aim(acid.red(), acid.green(), acid.blue(), acid.visibility(), ramp, seconds);
            } else {
                aim(((blend.rgb() >> 16) & 0xFF) / 255.0F,
                        ((blend.rgb() >> 8) & 0xFF) / 255.0F, (blend.rgb() & 0xFF) / 255.0F,
                        blend.visibility(), ramp, seconds);
            }
            submersion = FogMath.ease(submersion, 1.0F, seconds, SUBMERSION_TAU);
        } else {
            // Air: the world's own fog is left alone, but the authority keeps easing its state
            // toward "no medium" so that breaking the surface is a fade and re-entering starts
            // from where the eye left off.
            easedRed = FogMath.ease(easedRed, FogMath.clamp01(red), seconds, COLOUR_TAU);
            easedGreen = FogMath.ease(easedGreen, FogMath.clamp01(green), seconds, COLOUR_TAU);
            easedBlue = FogMath.ease(easedBlue, FogMath.clamp01(blue), seconds, COLOUR_TAU);
            easedVeil = FogMath.ease(easedVeil, 0.0F, seconds, SUBMERSION_TAU);
            submersion = FogMath.ease(submersion, 0.0F, seconds, SUBMERSION_TAU);
        }

        snapshot = new Snapshot(medium, medium.submerged(), submersion,
                easedRed, easedGreen, easedBlue, easedNear, easedFar, ramp, easedVeil);
        return snapshot;
    }

    /**
     * Ease everything the authority owns toward one medium's look. The first submerged frame
     * snaps rather than fades: there is nothing yet to fade from, and a fade in from black on
     * world join would read as a bug.
     */
    private static void aim(float red, float green, float blue, FogVisibility visibility,
                            float ramp, float seconds) {
        float near = FogMath.nearPlane(visibility, ramp);
        float far = FogMath.farPlane(visibility, ramp);
        float veil = FogMath.veilAlpha(visibility, ramp, FogClientConfig.castStrength());
        if (!seeded) {
            easedRed = red;
            easedGreen = green;
            easedBlue = blue;
            easedNear = near;
            easedFar = far;
            easedVeil = veil;
            seeded = true;
            return;
        }
        easedRed = FogMath.ease(easedRed, red, seconds, COLOUR_TAU);
        easedGreen = FogMath.ease(easedGreen, green, seconds, COLOUR_TAU);
        easedBlue = FogMath.ease(easedBlue, blue, seconds, COLOUR_TAU);
        easedNear = FogMath.ease(easedNear, near, seconds, PLANE_TAU);
        easedFar = FogMath.ease(easedFar, far, seconds, PLANE_TAU);
        easedVeil = FogMath.ease(easedVeil, veil, seconds, SUBMERSION_TAU);
    }

    /**
     * Which medium the camera sits in. Acid wins over water (a corrosive pool is not the
     * biome's sea), and it is found by tag because no vanilla {@code FogType} describes it.
     */
    public static FogMedium mediumAt(Camera camera) {
        if (camera == null || camera.getEntity() == null) {
            return FogMedium.AIR;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return FogMedium.AIR;
        }
        BlockPos eye = BlockPos.containing(camera.getPosition());
        if (level.getFluidState(eye).is(TagRegistry.SULFURIC_ACID)) {
            double surface = eye.getY() + level.getFluidState(eye).getHeight(level, eye);
            if (camera.getPosition().y < surface) {
                return FogMedium.ACID;
            }
        }
        return camera.getFluidInCamera() == FogType.WATER ? FogMedium.WATER : FogMedium.AIR;
    }

    public static float rampFor(Camera camera) {
        return FogMath.ramp(pressure(camera), drowningDarkness, nightVision(camera));
    }

    /** The client's own frame timer, in ticks: equal for every lookup within one frame. */
    private static float frameTicks() {
        return Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
    }

    private static double pressure(Camera camera) {
        Entity entity = camera.getEntity();
        return entity == null ? 0.0D : PressureHelper.getPressure(entity);
    }

    private static boolean nightVision(Camera camera) {
        return camera.getEntity() instanceof LivingEntity living
                && living.hasEffect(MobEffects.NIGHT_VISION);
    }

    /** The air snapshot, for callers that need to clear the frame's fog. */
    public static Snapshot air() {
        return AIR;
    }
}