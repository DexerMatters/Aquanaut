package com.dexer.aquanaut.client.renderer;

import java.util.List;

import com.dexer.aquanaut.common.entity.BiologicalDetectorEntity;
import com.dexer.aquanaut.common.entity.DetectorScan;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The sphere of light a working detector projects around itself.
 *
 * <p>
 * It is a map, not a decoration. What is inside the sphere is the water around the buoy, shrunk by
 * {@link DetectorScan#scale()} until forty-eight blocks of ocean fit in a three-and-a-half block
 * ball: a fish eight blocks away on the player's left is drawn a hand's width to their left, a
 * creature directly above the buoy is drawn above the middle of the sphere, and the sphere's own
 * floor is the buoy's own depth. Everything is in world-aligned axes, so the picture does not turn
 * with the instrument the way the hull does — north inside the sphere is north outside it, and a
 * player can read a bearing off it and swim it.
 *
 * <p>
 * The instrument is drawn in four passes, each doing one job:
 *
 * <ul>
 * <li>the <em>shell</em> is a fresnel-weighted fill, so the glass is bright where the eye skims it
 * and nearly absent where the eye looks straight through, which is what a bubble looks like and what
 * keeps the middle of the picture clear;</li>
 * <li>the <em>grid</em> is the latitude and longitude wireframe, the equator picked out, with four
 * ticks on the compass points: the reference frame the returns are read against;</li>
 * <li>the <em>sweep</em> is a disc of light standing on the bearing the hull is pointing at, with a
 * bright rim, a bright arm out to the edge, and a short wake behind it — the scan, plotted on the
 * same yaw the model is turned by, so the ball the player watches spin and the blade going round
 * inside the sphere are one motion;</li>
 * <li>the <em>returns</em>, one per creature in range, coloured by what it is, dimmed to almost
 * nothing and re-lit as the beam goes past, with a stem down to the mid-plane so the eye can read
 * how deep a contact is and a ring that expands away from the beam for a moment after it passes.</li>
 * </ul>
 *
 * <p>
 * All of it is additive and writes no depth, so it layers over the water without hiding it and a
 * diver who swims inside is not swallowed by the sphere they are standing in.
 */
final class DetectorHologram {

    // ------------------------------------------------------------------
    // palette
    // ------------------------------------------------------------------

    private static final float SHELL_RED = 0.26F;
    private static final float SHELL_GREEN = 0.74F;
    private static final float SHELL_BLUE = 0.96F;
    private static final float GRID_RED = 0.30F;
    private static final float GRID_GREEN = 0.86F;
    private static final float GRID_BLUE = 0.98F;
    private static final float RING_RED = 0.62F;
    private static final float RING_GREEN = 0.99F;
    private static final float RING_BLUE = 1.00F;

    // ------------------------------------------------------------------
    // shell
    // ------------------------------------------------------------------

    private static final int SHELL_STACKS = 18;
    private static final int SHELL_SLICES = 28;
    private static final float SHELL_ALPHA = 0.20F;
    private static final float SHELL_CORE = 0.13F;

    // ------------------------------------------------------------------
    // grid
    // ------------------------------------------------------------------

    private static final int PARALLELS = 5;
    private static final int MERIDIANS = 12;
    private static final int PARALLEL_SEGMENTS = 26;
    private static final int MERIDIAN_SEGMENTS = 16;
    private static final int EQUATOR_SEGMENTS = 64;
    private static final float GRID_ALPHA = 0.16F;
    private static final float EQUATOR_ALPHA = 0.34F;
    private static final float GRID_BACK = 0.26F;

    // ------------------------------------------------------------------
    // sweep
    // ------------------------------------------------------------------

    private static final int SWEEP_SEGMENTS = 56;
    private static final int SWEEP_TRAIL = 3;
    private static final float SWEEP_TRAIL_STEP = 11.0F;
    private static final float SWEEP_DISC_CENTRE = 0.014F;
    private static final float SWEEP_DISC_RIM = 0.090F;
    private static final float SWEEP_RIM_ALPHA = 0.62F;
    private static final float SWEEP_RIM_GLOW = 0.16F;
    private static final float SWEEP_ARM_ALPHA = 0.34F;

    // ------------------------------------------------------------------
    // returns
    // ------------------------------------------------------------------

    private static final float CONTACT_CORE_RADIUS = 0.032F;
    private static final float CONTACT_HALO_RADIUS = 0.085F;
    private static final float CONTACT_CORE_ALPHA = 0.95F;
    private static final float CONTACT_HALO_ALPHA = 0.42F;
    private static final float CONTACT_STEM_ALPHA = 0.24F;
    private static final float CONTACT_PING_ALPHA = 0.40F;
    private static final int CONTACT_SEGMENTS = 14;
    private static final int PING_SEGMENTS = 26;

    /**
     * How close to the eye a return stops being drawn, in hologram blocks, and where it is fully
     * back.
     *
     * <p>
     * A billboard keeps its size on screen by being small in the world, and the one thing that
     * breaks that is the eye standing on top of it: a creature a block from the player's face, or
     * the player's own return — which is always a hand's width under the camera — would otherwise
     * bloom into a disc across the whole screen. Returns fade out as the eye closes on them, which
     * is also the point at which the player can simply look at the creature.
     */
    private static final float CONTACT_NEAR_FADE_START = 0.09F;
    private static final float CONTACT_NEAR_FADE_END = 0.30F;

    // ------------------------------------------------------------------
    // line weight
    // ------------------------------------------------------------------

    /**
     * Half-width of a grid ribbon per block of distance from the eye.
     *
     * <p>
     * Lines are drawn as camera-facing ribbons rather than as GL lines, because the width of a GL
     * line is a driver's opinion and this has to be a hairline on every machine. Scaling the width
     * with the distance makes the ribbon subtend a constant angle, so the wireframe stays a
     * wireframe whether the player is reading it from across the water or swimming through it —
     * without it, a line that is a hairline at ten blocks is a stripe across the screen at one.
     */
    private static final float LINE_HALF_WIDTH_PER_BLOCK = 0.0019F;

    /** Floor for the above, so a ribbon the eye is standing on does not collapse to nothing. */
    private static final float MIN_LINE_HALF_WIDTH = 1.0E-4F;

    // ------------------------------------------------------------------
    // visibility
    // ------------------------------------------------------------------

    /**
     * Distance at which the projection starts to thin out.
     *
     * <p>
     * The hull is half a block across, so vanilla stops drawing the detector at thirty-two blocks;
     * the projection is faded out just inside that, so an instrument at the edge of its own draw
     * distance thins away rather than blinking off.
     */
    private static final float FADE_START = 22.0F;

    /** Distance past which it is gone: a hologram is a reading, not a lighthouse. */
    private static final float FADE_END = 31.0F;

    /**
     * The fog range the hologram is drawn with, in blocks.
     *
     * <p>
     * Far enough out that the fade is a flat one at any distance a projection is ever drawn at, and
     * nowhere near {@code Float.MAX_VALUE}, which would hand the shader a zero-width smoothstep.
     */
    private static final float FOGLESS_START = 1.0E9F;
    private static final float FOGLESS_END = 2.0E9F;

    /**
     * How much brighter the projection is written for a shader pipeline than for the ordinary game.
     *
     * <p>
     * A pipeline lights its buffer and then tone maps it, so an effect that adds a twentieth of its
     * colour to black water reads perfectly in the ordinary game and vanishes in the pack; a
     * projection has to arrive there at something like full strength. The vertex colours are only
     * eight bits, which leaves no room to write one picture for the game and a brighter one for the
     * pipeline — so this writes a brighter <em>colour</em> and puts the reciprocal in the alpha,
     * where the ordinary game's blend undoes it and a pipeline's forced-opaque buffer cannot.
     */
    private static final float PIPELINE_GAIN = 4.0F;

    private static final Vec3 X_AXIS = new Vec3(1.0D, 0.0D, 0.0D);
    private static final Vec3 Z_AXIS = new Vec3(0.0D, 0.0D, 1.0D);
    private static final Vec3 UP = new Vec3(0.0D, 1.0D, 0.0D);

    private DetectorHologram() {
    }

    /**
     * Draws the whole projection for {@code detector}, in a pose that has already been moved to the
     * buoy's own origin.
     */
    static void render(BiologicalDetectorEntity detector, PoseStack poseStack,
            MultiBufferSource bufferSource, float partialTick) {
        float boot = detector.hologramBoot(partialTick);
        if (boot <= 0.0F) {
            return;
        }

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 centre = detector.getPosition(partialTick).add(0.0D, DetectorScan.SPHERE_CENTRE_Y, 0.0D);
        Vec3 eye = camera.getPosition().subtract(centre);

        float distance = (float) eye.length();
        float fade = Mth.clamp((FADE_END - distance) / (FADE_END - FADE_START), 0.0F, 1.0F) * boot;
        if (fade <= 0.002F) {
            return;
        }

        // It comes up swelling rather than blinking on, in step with the clamshell still opening.
        float radius = DetectorScan.SPHERE_RADIUS
                * (DetectorScan.BOOT_START_SCALE + (1.0F - DetectorScan.BOOT_START_SCALE) * boot);
        float yaw = detector.getViewYRot(partialTick);
        float age = detector.tickCount + partialTick;

        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        Matrix4f matrix = poseStack.last().pose();

        // Asking for the buffer first is deliberate: the source flushes whatever else is still
        // pending in the shared buffer when the type changes, and that flush is somebody else's
        // geometry — GeckoLib's own glowmask, most likely — which must go out under the world's
        // fog and not under this.
        VertexConsumer consumer = bufferSource.getBuffer(DetectorRenderTypes.hologram());
        Canvas canvas = new Canvas(consumer, matrix, eye,
                new Vec3(left.x(), left.y(), left.z()),
                new Vec3(up.x(), up.y(), up.z()));

        // A projection is its own light and must not be eaten by the water it is read in. The
        // borrowed program is one that shader pipelines light and unfog themselves, but in the
        // ordinary game it is fogged — and this mod's abyss closes to a few blocks, which would
        // swallow the hologram exactly where it is most useful. The fog is pushed out of the way for
        // this one draw and put straight back.
        float fogStart = RenderSystem.getShaderFogStart();
        float fogEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(FOGLESS_START);
        RenderSystem.setShaderFogEnd(FOGLESS_END);
        try {
            drawShell(canvas, radius, age, fade);
            drawGrid(canvas, radius, fade);
            drawSweep(canvas, radius, yaw, fade);
            drawContacts(canvas, detector, centre, radius, yaw, partialTick, fade);

            // The fog uniforms are read when a batch is set up, so the batch has to leave now, while
            // they are still out of the way. Nothing else is holding this render type, so this draws
            // the hologram and only the hologram.
            if (bufferSource instanceof MultiBufferSource.BufferSource pending) {
                pending.endBatch(DetectorRenderTypes.hologram());
            }
        } finally {
            RenderSystem.setShaderFogStart(fogStart);
            RenderSystem.setShaderFogEnd(fogEnd);
        }
    }

    // ------------------------------------------------------------------
    // the glass
    // ------------------------------------------------------------------

    /**
     * The sphere itself: a closed surface whose alpha is a function of how obliquely the eye meets
     * it.
     *
     * <p>
     * Face-on it is almost nothing, at the silhouette it is bright — the fresnel rim that makes a
     * soap bubble a bubble. Both hemispheres are drawn (the render type does not cull), so the two
     * rims add where the sphere's edge is, which is the soft double line the eye reads as a glass
     * shell rather than as a painted circle. A slow breath in the brightness keeps the projection
     * feeling powered.
     */
    private static void drawShell(Canvas canvas, float radius, float age, float fade) {
        float pulse = 0.93F + 0.07F * Mth.sin(age * 0.11F);
        float glow = fade * pulse;
        for (int stack = 0; stack < SHELL_STACKS; stack++) {
            float polarLow = Mth.PI * stack / SHELL_STACKS;
            float polarHigh = Mth.PI * (stack + 1) / SHELL_STACKS;
            for (int slice = 0; slice < SHELL_SLICES; slice++) {
                float azimuthLow = Mth.TWO_PI * slice / SHELL_SLICES;
                float azimuthHigh = Mth.TWO_PI * (slice + 1) / SHELL_SLICES;

                Vec3 lowLow = sphere(polarLow, azimuthLow, radius);
                Vec3 lowHigh = sphere(polarLow, azimuthHigh, radius);
                Vec3 highHigh = sphere(polarHigh, azimuthHigh, radius);
                Vec3 highLow = sphere(polarHigh, azimuthLow, radius);

                canvas.quad(lowLow, lowHigh, highHigh, highLow,
                        SHELL_RED, SHELL_GREEN, SHELL_BLUE,
                        rimGlow(canvas, lowLow, glow),
                        rimGlow(canvas, lowHigh, glow),
                        rimGlow(canvas, highHigh, glow),
                        rimGlow(canvas, highLow, glow));
            }
        }
    }

    private static float rimGlow(Canvas canvas, Vec3 point, float glow) {
        Vec3 view = canvas.toCamera(point);
        double length = view.length();
        if (length < 1.0E-6D) {
            return 0.0F;
        }
        float facing = (float) Math.abs(point.normalize().dot(view.scale(1.0D / length)));
        float rim = 1.0F - facing;
        return SHELL_ALPHA * (SHELL_CORE + (1.0F - SHELL_CORE) * rim * rim) * glow;
    }

    // ------------------------------------------------------------------
    // the reference frame
    // ------------------------------------------------------------------

    private static void drawGrid(Canvas canvas, float radius, float fade) {
        for (int i = 1; i <= PARALLELS; i++) {
            float polar = Mth.PI * i / (PARALLELS + 1);
            boolean equator = i * 2 == PARALLELS + 1;
            canvas.ring(new Vec3(0.0D, radius * Mth.cos(polar), 0.0D), X_AXIS, Z_AXIS,
                    radius * Mth.sin(polar),
                    equator ? EQUATOR_SEGMENTS : PARALLEL_SEGMENTS,
                    GRID_RED, GRID_GREEN, GRID_BLUE,
                    (equator ? EQUATOR_ALPHA : GRID_ALPHA) * fade,
                    equator ? 1.5F : 1.0F, true);
        }

        for (int meridian = 0; meridian < MERIDIANS; meridian++) {
            float azimuth = Mth.TWO_PI * meridian / MERIDIANS;
            Vec3 previous = null;
            for (int step = 0; step <= MERIDIAN_SEGMENTS; step++) {
                Vec3 point = sphere(Mth.PI * step / MERIDIAN_SEGMENTS, azimuth, radius);
                if (previous != null) {
                    float glow = canvas.polarGlow(previous, point, GRID_ALPHA * fade);
                    canvas.ribbon(previous, point, GRID_RED, GRID_GREEN, GRID_BLUE, glow, glow, 1.0F);
                }
                previous = point;
            }
        }

        // Compass ticks, so the map has a north to read a bearing against.
        for (int point = 0; point < 4; point++) {
            float azimuth = Mth.TWO_PI * point / 4.0F;
            Vec3 direction = new Vec3(Mth.cos(azimuth), 0.0D, Mth.sin(azimuth));
            canvas.ribbon(direction.scale(radius * 0.84D), direction.scale(radius),
                    RING_RED, RING_GREEN, RING_BLUE, 0.42F * fade, 0.54F * fade, 1.3F);
        }
    }

    // ------------------------------------------------------------------
    // the scan
    // ------------------------------------------------------------------

    /**
     * A blade of light standing on the hull's bearing, and the wake of the last few it swept.
     *
     * <p>
     * The blade is a disc in the vertical plane through the beam's bearing, which is the honest shape
     * for it: a scan at one azimuth covers every depth on that azimuth, so the plane of the sweep is
     * a full circle stood on edge. Its rim is bright and its face is barely there, which reads as
     * the edge of the beam rather than a wall of light across the picture. The ghosts behind it are
     * the same blade a moment ago, fading with the square of their age, and the arm out from the
     * centre is what makes the direction of rotation unmistakable in a still frame.
     */
    private static void drawSweep(Canvas canvas, float radius, float yaw, float fade) {
        Vec3 leading = Vec3.directionFromRotation(0.0F, yaw);
        canvas.disc(Vec3.ZERO, leading, UP, radius, SWEEP_SEGMENTS,
                RING_RED, RING_GREEN, RING_BLUE, SWEEP_DISC_CENTRE * fade, SWEEP_DISC_RIM * fade);
        canvas.ring(Vec3.ZERO, leading, UP, radius, SWEEP_SEGMENTS,
                RING_RED, RING_GREEN, RING_BLUE, SWEEP_RIM_GLOW * fade, 4.0F, false);
        canvas.ring(Vec3.ZERO, leading, UP, radius, SWEEP_SEGMENTS,
                RING_RED, RING_GREEN, RING_BLUE, SWEEP_RIM_ALPHA * fade, 1.7F, false);
        canvas.ribbon(Vec3.ZERO, leading.scale(radius), RING_RED, RING_GREEN, RING_BLUE,
                SWEEP_ARM_ALPHA * fade * 0.45F, SWEEP_ARM_ALPHA * fade, 1.2F);

        for (int trail = 1; trail <= SWEEP_TRAIL; trail++) {
            float weight = 1.0F - trail / (float) (SWEEP_TRAIL + 1);
            weight *= weight;
            Vec3 behind = Vec3.directionFromRotation(0.0F, yaw - trail * SWEEP_TRAIL_STEP);
            canvas.disc(Vec3.ZERO, behind, UP, radius, SWEEP_SEGMENTS,
                    RING_RED, RING_GREEN, RING_BLUE,
                    SWEEP_DISC_CENTRE * fade * weight, SWEEP_DISC_RIM * fade * weight);
            canvas.ring(Vec3.ZERO, behind, UP, radius, SWEEP_SEGMENTS,
                    RING_RED, RING_GREEN, RING_BLUE, SWEEP_RIM_ALPHA * fade * weight * 0.65F, 1.1F, false);
        }
    }

    // ------------------------------------------------------------------
    // the returns
    // ------------------------------------------------------------------

    private static void drawContacts(Canvas canvas, BiologicalDetectorEntity detector, Vec3 centre,
            float radius, float yaw, float partialTick, float fade) {
        List<LivingEntity> contacts = DetectorContacts.of(detector);
        if (contacts.isEmpty()) {
            return;
        }

        Vec3 sweep = Vec3.directionFromRotation(0.0F, yaw);
        float sweepBearing = (float) Math.toDegrees(Math.atan2(sweep.z, sweep.x));
        float scale = DetectorScan.scale();

        for (LivingEntity contact : contacts) {
            Vec3 local = contact.getPosition(partialTick).subtract(centre).scale(scale);
            double distance = local.length();
            if (distance > radius) {
                // A creature on the very edge is still a creature; pull it onto the shell rather
                // than dropping it, so the plot loses returns to nothing but rounding.
                local = local.scale(radius / distance);
            }
            if (local.lengthSqr() < 1.0E-8D) {
                continue;
            }

            float near = Mth.clamp(
                    (float) (local.distanceTo(canvas.eye) - CONTACT_NEAR_FADE_START)
                            / (CONTACT_NEAR_FADE_END - CONTACT_NEAR_FADE_START),
                    0.0F, 1.0F);
            if (near <= 0.0F) {
                continue;
            }

            float lag = DetectorScan.sweepLag(sweepBearing,
                    (float) Math.toDegrees(Math.atan2(local.z, local.x)));
            float freshness = 1.0F - lag / 360.0F;
            float pulse = DetectorScan.CONTACT_BASE_GLOW + (1.0F - DetectorScan.CONTACT_BASE_GLOW)
                    * (float) Math.pow(freshness, 5.0D);
            float glow = fade * pulse * near;

            DetectorSignal signal = DetectorSignal.of(contact);
            float red = signal.red;
            float green = signal.green;
            float blue = signal.blue;

            // A stem down to the middle of the sphere, so the eye reads height without guessing:
            // short above the plane, long below it, gone when the contact is level with the buoy.
            if (Math.abs(local.y) > 0.004D) {
                canvas.ribbon(new Vec3(local.x, 0.0D, local.z), local, red, green, blue,
                        CONTACT_STEM_ALPHA * glow * 0.3F, CONTACT_STEM_ALPHA * glow, 0.7F);
            }

            // Two layers of glow around a hard centre. The tight one gives the return its body and
            // the wide one is what carries it across the sphere without having to make the dot
            // itself any bigger, which is how a lit point actually reads.
            canvas.disc(local, canvas.screenRight, canvas.screenUp, CONTACT_HALO_RADIUS * 1.9F,
                    CONTACT_SEGMENTS, red, green, blue, CONTACT_HALO_ALPHA * 0.3F * glow, 0.0F);
            canvas.disc(local, canvas.screenRight, canvas.screenUp, CONTACT_HALO_RADIUS,
                    CONTACT_SEGMENTS, red, green, blue, CONTACT_HALO_ALPHA * glow, 0.0F);
            canvas.disc(local, canvas.screenRight, canvas.screenUp, CONTACT_CORE_RADIUS,
                    CONTACT_SEGMENTS, red, green, blue,
                    CONTACT_CORE_ALPHA * glow, CONTACT_CORE_ALPHA * glow * 0.35F);

            if (lag < DetectorScan.CONTACT_PING_SPAN) {
                float age = lag / DetectorScan.CONTACT_PING_SPAN;
                float ringRadius = CONTACT_HALO_RADIUS * (0.7F + 1.9F * age);
                float ringAlpha = CONTACT_PING_ALPHA * (1.0F - age) * (1.0F - age) * fade * near;
                canvas.ring(local, canvas.screenRight, canvas.screenUp, ringRadius, PING_SEGMENTS,
                        red, green, blue, ringAlpha, 0.8F, false);
            }
        }
    }

    // ------------------------------------------------------------------
    // geometry helper
    // ------------------------------------------------------------------

    /** A point on the sphere, polar angle from the north pole and azimuth around it. */
    private static Vec3 sphere(float polar, float azimuth, float radius) {
        float ring = Mth.sin(polar) * radius;
        return new Vec3(ring * Mth.cos(azimuth), Mth.cos(polar) * radius, ring * Mth.sin(azimuth));
    }

    /**
     * The buffer, the frame it is being written in, and where the eye is inside that frame.
     *
     * <p>
     * Everything the hologram is made of reduces to two primitives: a camera-facing ribbon for
     * anything long and thin, and a camera-facing fan for anything round. Both are built in the
     * hologram's own coordinates and handed straight to the consumer, and both ask the eye where it
     * is, which is why it is carried around rather than passed back in.
     */
    private static final class Canvas {

        private final VertexConsumer consumer;
        private final Matrix4f matrix;
        private final Vec3 eye;
        private final Vec3 screenRight;
        private final Vec3 screenUp;

        Canvas(VertexConsumer consumer, Matrix4f matrix, Vec3 eye, Vec3 screenRight, Vec3 screenUp) {
            this.consumer = consumer;
            this.matrix = matrix;
            this.eye = eye;
            this.screenRight = screenRight;
            this.screenUp = screenUp;
        }

        Vec3 toCamera(Vec3 point) {
            return this.eye.subtract(point);
        }

        void vertex(Vec3 point, float red, float green, float blue, float alpha) {
            // The colour is written amplified and the alpha as the reciprocal of that
            // amplification, so the two multiply back to exactly the designed intensity under the
            // alpha-weighted blend — the ordinary game is unchanged to the bit. A shader pipeline
            // writes its buffer with the alpha forced opaque, which drops the undoing half and
            // leaves the amplified colour, structure and all: the faint end of a fresnel skirt is
            // lifted instead of being crushed to nothing. See DetectorRenderTypes.
            float peak = Math.max(red, Math.max(green, blue)) * alpha;
            float gain = peak > 1.0E-5F ? Math.min(PIPELINE_GAIN, 1.0F / peak) : PIPELINE_GAIN;
            Vec3 view = this.toCamera(point);
            float normalX = 0.0F;
            float normalY = 1.0F;
            float normalZ = 0.0F;
            if (view.lengthSqr() > 1.0E-12D) {
                Vec3 normal = view.normalize();
                normalX = (float) normal.x;
                normalY = (float) normal.y;
                normalZ = (float) normal.z;
            }
            this.consumer.addVertex(this.matrix, (float) point.x, (float) point.y, (float) point.z)
                    .setColor(red * alpha * gain, green * alpha * gain, blue * alpha * gain, 1.0F / gain)
                    .setUv(0.0F, 0.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(normalX, normalY, normalZ);
        }

        void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, float red, float green, float blue,
                float alphaA, float alphaB, float alphaC, float alphaD) {
            this.vertex(a, red, green, blue, alphaA);
            this.vertex(b, red, green, blue, alphaB);
            this.vertex(c, red, green, blue, alphaC);
            this.vertex(d, red, green, blue, alphaD);
        }

        /**
         * A hairline from {@code from} to {@code to}, turned to face the eye and weighted by the
         * distance to each end so it keeps a constant width on screen.
         */
        void ribbon(Vec3 from, Vec3 to, float red, float green, float blue,
                float alphaFrom, float alphaTo, float widthScale) {
            Vec3 direction = to.subtract(from);
            if (direction.lengthSqr() < 1.0E-10D) {
                return;
            }
            direction = direction.normalize();

            Vec3 side = direction.cross(this.toCamera(from.add(to).scale(0.5D)));
            if (side.lengthSqr() < 1.0E-10D) {
                // The segment is pointing at the eye: any perpendicular will do.
                side = direction.cross(UP);
                if (side.lengthSqr() < 1.0E-10D) {
                    side = direction.cross(X_AXIS);
                }
            }
            side = side.normalize();

            double fromHalf = this.halfWidth(from) * widthScale;
            double toHalf = this.halfWidth(to) * widthScale;
            this.quad(from.subtract(side.scale(fromHalf)), from.add(side.scale(fromHalf)),
                    to.add(side.scale(toHalf)), to.subtract(side.scale(toHalf)),
                    red, green, blue, alphaFrom, alphaFrom, alphaTo, alphaTo);
        }

        /**
         * A camera-facing circle of light, brightest at the middle and fading to its rim.
         *
         * <p>
         * Written as degenerate quads — the third corner repeated — because the whole hologram is
         * drawn through one quad-mode buffer; a repeated corner is how a triangle fits in it.
         */
        void disc(Vec3 centre, Vec3 axisU, Vec3 axisV, float radius, int segments,
                float red, float green, float blue, float centreAlpha, float rimAlpha) {
            float step = Mth.TWO_PI / segments;
            Vec3 previous = centre.add(axisU.scale(radius));
            for (int i = 1; i <= segments; i++) {
                float angle = step * i;
                Vec3 point = centre.add(axisU.scale(radius * Mth.cos(angle)))
                        .add(axisV.scale(radius * Mth.sin(angle)));
                this.vertex(centre, red, green, blue, centreAlpha);
                this.vertex(previous, red, green, blue, rimAlpha);
                this.vertex(point, red, green, blue, rimAlpha);
                this.vertex(point, red, green, blue, rimAlpha);
                previous = point;
            }
        }

        /**
         * A circle in an arbitrary plane, drawn as a ribbon.
         *
         * @param shaded whether the far half is dimmed, which is what sells a ring as lying on the
         *               sphere rather than floating in front of it
         */
        void ring(Vec3 centre, Vec3 axisU, Vec3 axisV, float radius, int segments,
                float red, float green, float blue, float alpha, float widthScale, boolean shaded) {
            Vec3 previous = null;
            for (int i = 0; i <= segments; i++) {
                float angle = Mth.TWO_PI * i / segments;
                Vec3 point = centre.add(axisU.scale(radius * Mth.cos(angle)))
                        .add(axisV.scale(radius * Mth.sin(angle)));
                if (previous != null) {
                    float glow = shaded ? this.faceGlow(previous, point, alpha) : alpha;
                    this.ribbon(previous, point, red, green, blue, glow, glow, widthScale);
                }
                previous = point;
            }
        }

        /** {@link #faceGlow} with the ends of the sphere's own meridians faded out of the poles. */
        float polarGlow(Vec3 from, Vec3 to, float alpha) {
            Vec3 midpoint = from.add(to).scale(0.5D);
            double length = midpoint.length();
            if (length < 1.0E-6D) {
                return 0.0F;
            }
            double horizontal = Math.sqrt(midpoint.x * midpoint.x + midpoint.z * midpoint.z);
            return this.faceGlow(from, to, (float) (alpha * horizontal / length));
        }

        private float faceGlow(Vec3 from, Vec3 to, float alpha) {
            Vec3 midpoint = from.add(to).scale(0.5D);
            double length = midpoint.length();
            if (length < 1.0E-6D) {
                return alpha;
            }
            Vec3 view = this.toCamera(midpoint);
            double viewLength = view.length();
            if (viewLength < 1.0E-6D) {
                return alpha;
            }
            Vec3 normal = midpoint.scale(1.0D / length);
            float facing = (float) Math.max(0.0D, normal.dot(view) / viewLength);
            return alpha * (GRID_BACK + (1.0F - GRID_BACK) * facing);
        }

        private double halfWidth(Vec3 point) {
            return Math.max(MIN_LINE_HALF_WIDTH,
                    this.toCamera(point).length() * LINE_HALF_WIDTH_PER_BLOCK);
        }
    }
}
