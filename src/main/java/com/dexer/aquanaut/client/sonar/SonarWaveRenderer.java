package com.dexer.aquanaut.client.sonar;

import java.util.List;

import org.joml.Matrix4f;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.sonar.SonarPulse;
import com.dexer.aquanaut.common.sonar.SonarReturn;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The pulse, in the water.
 *
 * <p>
 * A ping is drawn as three things, one after another: a shell of light leaving the transducer and
 * going out, a ring where that shell cuts the horizontal, and then a comet for every contact the
 * shell has passed, running back the way the sound came. The shell is a fresnel surface — almost
 * nothing where it faces the eye and bright at its rim, which is what makes a bubble a bubble and
 * what tells the diver they are standing inside a sphere rather than looking at a disc. The comets
 * are the instrument talking: each one is a contact saying "here", in its own colour, from the
 * direction it will be heard from when its echo arrives.
 *
 * <p>
 * Nothing here is simulated. The whole picture is a pure function of one number — the age of the
 * ping — taken from {@link SonarPulse}, so the ring in the water and the ring on the scope are the
 * same ring, drawn twice, and can never disagree about where the pulse is.
 *
 * <p>
 * Drawn at {@link RenderLevelStageEvent.Stage#AFTER_PARTICLES}, so the water is complete under it
 * and the hand is drawn over it, with depth testing on: the wave travels through water and around
 * corners, but never through rock.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, value = Dist.CLIENT)
public final class SonarWaveRenderer {

    // ------------------------------------------------------------------
    // tessellation
    // ------------------------------------------------------------------

    /** Bands of latitude in the shell. Enough that the rim reads as a circle, not a polygon. */
    private static final int SHELL_STACKS = 12;

    /** Steps around the shell. */
    private static final int SHELL_SLICES = 24;

    /** Steps around a ring or a horizon circle. */
    private static final int RING_SEGMENTS = 64;

    /** Steps around a billboard disc. */
    private static final int DISC_SEGMENTS = 20;

    // ------------------------------------------------------------------
    // the shell
    // ------------------------------------------------------------------

    /** How bright the shell is where the eye meets it edge-on. */
    private static final float SHELL_RIM = 0.34F;

    /**
     * How much of that brightness survives where the eye meets it face-on. Not zero: a shell that
     * vanishes when looked at straight on reads as a hole, and the diver's own side of the sphere
     * has to be faintly there for the far side to look like the far side.
     */
    private static final float SHELL_FACE = 0.05F;

    /**
     * How much brighter the shell is in the horizontal than at its poles. A sonar pulse is a sphere,
     * but the eye reads the equator as the sweep and the poles as nothing much, so the band is
     * weighted to where the information is.
     */
    private static final float SHELL_BAND = 0.6F;

    // ------------------------------------------------------------------
    // the contacts
    // ------------------------------------------------------------------

    /** How many contacts get a comet. A ping that found a whole reef should be a picture, not a web. */
    private static final int MAX_COMETS = 6;

    /** Half a comet's core, in blocks. */
    private static final float COMET_CORE = 0.17F;

    /** How much of the run home a comet's tail covers, as a fraction of the path. */
    private static final float COMET_TAIL = 0.28F;

    /** How bright a contact's flare is at the moment the pulse strikes it. */
    private static final float STRIKE_ALPHA = 0.5F;

    /** How bright a comet running home is. */
    private static final float COMET_ALPHA = 0.5F;

    /** How bright the horizon circle where the shell cuts the water is. */
    private static final float SWEEP_ALPHA = 0.42F;

    /** How bright the flash at the transducer is, and how long it lasts. */
    private static final float MUZZLE_ALPHA = 0.7F;
    private static final float MUZZLE_TICKS = 4.0F;

    // ------------------------------------------------------------------
    // line weight
    // ------------------------------------------------------------------

    /**
     * Half-width of a ribbon per block of distance from the eye, so a line keeps a constant width on
     * screen instead of becoming a stripe across the lens as the dive closes on it.
     */
    private static final float RIBBON_HALF_WIDTH_PER_BLOCK = 0.0035F;

    /** Floor for the above, so a ribbon under the diver's nose does not collapse to nothing. */
    private static final float MIN_RIBBON_HALF_WIDTH = 1.5E-3F;

    /**
     * How much brighter the pulse is written for a shader pipeline than for the ordinary game. A
     * pipeline lights its buffer and then tone maps it, so an effect that adds a twentieth of its
     * colour to black water reads perfectly in the ordinary game and vanishes in the pack. The
     * vertex colours are eight bits, which leaves no room to write two pictures — so the colour is
     * written brighter and the reciprocal goes in the alpha, where the ordinary game's blend undoes
     * it and a forced-opaque buffer cannot.
     */
    private static final float PIPELINE_GAIN = 4.0F;

    /**
     * The fog range the pulse is drawn with, in blocks. A pulse is its own light and must not be
     * eaten by the water it is read in — this mod's abyss closes to a few blocks, which would
     * swallow the instrument exactly where it is most wanted. Nowhere near {@code Float.MAX_VALUE},
     * which would hand the shader a zero-width smoothstep.
     */
    private static final float FOGLESS_START = 1.0E9F;
    private static final float FOGLESS_END = 2.0E9F;

    // ------------------------------------------------------------------
    // colour
    // ------------------------------------------------------------------

    private static final float WAVE_RED = 0.46F;
    private static final float WAVE_GREEN = 0.88F;
    private static final float WAVE_BLUE = 1.0F;

    private static final Vec3 UP = new Vec3(0.0D, 1.0D, 0.0D);
    private static final Vec3 X_AXIS = new Vec3(1.0D, 0.0D, 0.0D);

    private SonarWaveRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || ClientSonarData.pictures().isEmpty()) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        Vec3 eye = camera.getPosition();
        Matrix4f matrix = event.getPoseStack().last().pose();

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        // Asking for the buffer first is deliberate: the source flushes whatever else is still
        // pending in the shared buffer when the type changes, and that flush is somebody else's
        // geometry, which must go out under the world's fog and not under this.
        VertexConsumer consumer = buffers.getBuffer(SonarRenderTypes.wave());
        Canvas canvas = new Canvas(consumer, matrix, Vec3.ZERO,
                new Vec3(camera.getLeftVector().x(), camera.getLeftVector().y(), camera.getLeftVector().z),
                new Vec3(camera.getUpVector().x(), camera.getUpVector().y(), camera.getUpVector().z));

        float fogStart = RenderSystem.getShaderFogStart();
        float fogEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(FOGLESS_START);
        RenderSystem.setShaderFogEnd(FOGLESS_END);

        try {
            for (ClientSonarData.Picture picture : ClientSonarData.pictures()) {
                draw(canvas, picture, eye, partialTick);
            }

            // The fog uniforms are read when a batch is set up, so the batch has to leave now,
            // while they are still out of the way.
            buffers.endBatch(SonarRenderTypes.wave());
        } finally {
            RenderSystem.setShaderFogStart(fogStart);
            RenderSystem.setShaderFogEnd(fogEnd);
        }
    }

    /** One ping, at whatever age it has reached. */
    private static void draw(Canvas canvas, ClientSonarData.Picture picture, Vec3 eye, float partialTick) {
        float age = picture.age(partialTick);
        Vec3 centre = picture.origin().subtract(eye);

        if (!SonarPulse.isAlive(age)) {
            return;
        }

        float glow = SonarPulse.waveGlow(age);
        float radius = (float) SonarPulse.wavefrontRadius(age);

        if (radius > 0.05F && SonarPulse.sweep(age) < 1.0F) {
            shell(canvas, centre, radius, glow);
            horizon(canvas, centre, radius, glow);
        }

        if (age < MUZZLE_TICKS) {
            muzzle(canvas, centre, age);
        }

        List<SonarReturn> contacts = picture.returns();

        for (int index = 0; index < contacts.size(); index++) {
            echo(canvas, centre, contacts.get(index), index, age);
        }
    }

    // ------------------------------------------------------------------
    // the shell
    // ------------------------------------------------------------------

    /**
     * The sphere itself: a closed surface whose alpha is a function of how obliquely the eye meets
     * it, banded towards the horizontal. Both sides are drawn — the render type does not cull — so
     * the near and far rims add where the sphere's edge is, which is the soft double line the eye
     * reads as a shell rather than as a painted circle.
     */
    private static void shell(Canvas canvas, Vec3 centre, float radius, float glow) {
        for (int stack = 0; stack < SHELL_STACKS; stack++) {
            float polarLow = Mth.PI * stack / SHELL_STACKS;
            float polarHigh = Mth.PI * (stack + 1) / SHELL_STACKS;
            float bandLow = band(polarLow);
            float bandHigh = band(polarHigh);

            for (int slice = 0; slice < SHELL_SLICES; slice++) {
                float azimuthLow = Mth.TWO_PI * slice / SHELL_SLICES;
                float azimuthHigh = Mth.TWO_PI * (slice + 1) / SHELL_SLICES;

                Vec3 lowLow = centre.add(sphere(polarLow, azimuthLow, radius));
                Vec3 lowHigh = centre.add(sphere(polarLow, azimuthHigh, radius));
                Vec3 highHigh = centre.add(sphere(polarHigh, azimuthHigh, radius));
                Vec3 highLow = centre.add(sphere(polarHigh, azimuthLow, radius));

                canvas.quad(lowLow, lowHigh, highHigh, highLow, WAVE_RED, WAVE_GREEN, WAVE_BLUE,
                        rim(canvas, lowLow, centre, glow * bandLow),
                        rim(canvas, lowHigh, centre, glow * bandLow),
                        rim(canvas, highHigh, centre, glow * bandHigh),
                        rim(canvas, highLow, centre, glow * bandHigh));
            }
        }
    }

    /**
     * The circle where the shell cuts the horizontal: the sweep the diver reads as the pulse going
     * out, and the one part of the sphere that a diver looking straight ahead can see all of.
     */
    private static void horizon(Canvas canvas, Vec3 centre, float radius, float glow) {
        canvas.ring(centre, UP, radius, RING_SEGMENTS, WAVE_RED, WAVE_GREEN, WAVE_BLUE,
                SWEEP_ALPHA * glow, 1.1F);
    }

    /** The flash at the transducer: the instrument letting go. */
    private static void muzzle(Canvas canvas, Vec3 centre, float age) {
        float t = Mth.clamp(age / MUZZLE_TICKS, 0.0F, 1.0F);
        float alpha = (1.0F - t) * (1.0F - t);

        canvas.disc(centre, canvas.screenRight, canvas.screenUp, 0.35F + 1.1F * t, DISC_SEGMENTS,
                WAVE_RED, WAVE_GREEN, WAVE_BLUE, MUZZLE_ALPHA * alpha, 0.0F);
        canvas.ring(centre, UP, 0.5F + 2.4F * t, RING_SEGMENTS, WAVE_RED, WAVE_GREEN, WAVE_BLUE,
                MUZZLE_ALPHA * alpha * 0.7F, 1.2F);
    }

    // ------------------------------------------------------------------
    // the echoes
    // ------------------------------------------------------------------

    /**
     * One contact: the flare where the pulse struck it, and the comet carrying the answer home.
     *
     * <p>
     * The colour is the voice's, so the picture in the water and the plot on the scope agree about
     * which kind of thing this is without either of them being told twice.
     */
    private static void echo(Canvas canvas, Vec3 centre, SonarReturn contact, int index, float age) {
        double distance = contact.distance();

        if (!SonarPulse.hasReached(age, distance)) {
            return;
        }

        float run = SonarPulse.echoProgress(age, distance);
        float glow = SonarPulse.echoGlow(age, distance, contact.strength());
        float red = contact.signal().red();
        float green = contact.signal().green();
        float blue = contact.signal().blue();
        Vec3 at = centre.add(contact.offset());

        // The strike: a ring thrown off the surface, and the flare at its heart. Both go out with
        // the echo — the water carries the event, and once the answer is home the event is over.
        // Holding the reading is the scope's job, not the sea's.
        float strike = 1.0F - run;
        canvas.ring(at, canvas.towards(at), 0.3F + 1.7F * run, DISC_SEGMENTS,
                red, green, blue, STRIKE_ALPHA * glow * strike * strike, 0.9F);
        canvas.disc(at, canvas.screenRight, canvas.screenUp, 0.16F + 0.22F * contact.strength(),
                DISC_SEGMENTS, red, green, blue, STRIKE_ALPHA * glow * strike, 0.0F);

        if (run >= 1.0F || index >= MAX_COMETS) {
            return;
        }

        // The comet: a mote running back down the ray it came in on, with a tail behind it.
        Vec3 head = centre.add(contact.offset().scale(1.0D - run));
        Vec3 tail = centre.add(contact.offset().scale(1.0D - Math.min(1.0D, run + COMET_TAIL)));
        float brightness = COMET_ALPHA * glow * (1.0F - 0.45F * run);

        canvas.ribbon(tail, head, red, green, blue, 0.0F, brightness, 1.0F);
        canvas.disc(head, canvas.screenRight, canvas.screenUp, COMET_CORE, DISC_SEGMENTS,
                red, green, blue, brightness * 1.35F, 0.0F);
    }

    // ------------------------------------------------------------------
    // shape helpers
    // ------------------------------------------------------------------

    /** A point on a sphere, polar angle from the north pole and azimuth around it. */
    private static Vec3 sphere(float polar, float azimuth, float radius) {
        float ring = Mth.sin(polar) * radius;
        return new Vec3(ring * Mth.cos(azimuth), Mth.cos(polar) * radius, ring * Mth.sin(azimuth));
    }

    /** How much of the shell's brightness a band of latitude carries. */
    private static float band(float polar) {
        return (1.0F - SHELL_BAND) + SHELL_BAND * Mth.sin(polar);
    }

    /** The fresnel weight of a point on the shell: bright at the rim, faint face-on. */
    private static float rim(Canvas canvas, Vec3 point, Vec3 centre, float glow) {
        Vec3 normal = point.subtract(centre);

        if (normal.lengthSqr() < 1.0E-9D) {
            return SHELL_FACE * glow;
        }

        normal = normal.normalize();
        Vec3 view = canvas.eye.subtract(point);

        if (view.lengthSqr() < 1.0E-9D) {
            return SHELL_RIM * glow;
        }

        double facing = Math.abs(normal.dot(view.normalize()));
        return glow * (SHELL_FACE + SHELL_RIM * (float) Math.pow(1.0D - facing, 2.6D));
    }

    // ------------------------------------------------------------------
    // geometry helper
    // ------------------------------------------------------------------

    /**
     * The buffer, the frame it is being written in, and where the eye is in that frame.
     *
     * <p>
     * Everything the pulse is made of reduces to three primitives: a camera-facing ribbon for
     * anything long and thin, a camera-facing fan for anything round, and a ring for anything that
     * has to lie in a plane. All of them are built in the pulse's own coordinates and handed to the
     * consumer, and all of them ask the eye where it is, which is why the eye is carried around
     * rather than passed back in at every call.
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

        /** The direction from a point to the eye, for billboarding a ring at it. */
        Vec3 towards(Vec3 point) {
            Vec3 view = this.eye.subtract(point);
            return view.lengthSqr() < 1.0E-9D ? UP : view.normalize();
        }

        /**
         * One vertex. The colour is written amplified and the alpha as the reciprocal of that
         * amplification, so the two multiply back to exactly the designed intensity under the
         * alpha-weighted blend, and a pipeline that forces its buffer opaque is left the amplified
         * colour. See {@link SonarRenderTypes}.
         */
        void vertex(Vec3 point, float red, float green, float blue, float alpha) {
            float peak = Math.max(red, Math.max(green, blue)) * alpha;
            float gain = peak > 1.0E-5F ? Math.min(PIPELINE_GAIN, 1.0F / peak) : PIPELINE_GAIN;
            Vec3 view = this.eye.subtract(point);
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
            vertex(a, red, green, blue, alphaA);
            vertex(b, red, green, blue, alphaB);
            vertex(c, red, green, blue, alphaC);
            vertex(d, red, green, blue, alphaD);
        }

        /**
         * A camera-facing triangle fan: a disc that keeps its shape from any angle, which is what a
         * return is — a point of light, not a surface.
         */
        void disc(Vec3 centre, Vec3 right, Vec3 up, float radius, int segments,
                float red, float green, float blue, float centreAlpha, float rimAlpha) {
            for (int segment = 0; segment < segments; segment++) {
                float low = Mth.TWO_PI * segment / segments;
                float high = Mth.TWO_PI * (segment + 1) / segments;
                Vec3 one = centre.add(right.scale(Mth.cos(low) * radius)).add(up.scale(Mth.sin(low) * radius));
                Vec3 other = centre.add(right.scale(Mth.cos(high) * radius)).add(up.scale(Mth.sin(high) * radius));
                quad(centre, one, other, centre, red, green, blue, centreAlpha, rimAlpha, rimAlpha, centreAlpha);
            }
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
            Vec3 side = direction.cross(this.towards(from.add(to).scale(0.5D)));

            if (side.lengthSqr() < 1.0E-10D) {
                side = direction.cross(UP);
            }

            if (side.lengthSqr() < 1.0E-10D) {
                side = direction.cross(X_AXIS);
            }

            side = side.normalize();
            float fromWidth = width(from, widthScale);
            float toWidth = width(to, widthScale);

            vertex(from.subtract(side.scale(fromWidth)), red, green, blue, alphaFrom);
            vertex(to.subtract(side.scale(toWidth)), red, green, blue, alphaTo);
            vertex(to.add(side.scale(toWidth)), red, green, blue, alphaTo);
            vertex(from.add(side.scale(fromWidth)), red, green, blue, alphaFrom);
        }

        /** A ribbon running through every segment of a circle lying in the plane normal to {@code axis}. */
        void ring(Vec3 centre, Vec3 axis, float radius, int segments,
                float red, float green, float blue, float alpha, float widthScale) {
            if (alpha <= 0.001F || radius <= 0.0F) {
                return;
            }

            Vec3 normal = axis.normalize();
            Vec3 spoke = normal.cross(UP);

            if (spoke.lengthSqr() < 1.0E-8D) {
                spoke = normal.cross(X_AXIS);
            }

            spoke = spoke.normalize();
            Vec3 other = normal.cross(spoke).normalize();
            Vec3 previous = null;

            for (int segment = 0; segment <= segments; segment++) {
                float angle = Mth.TWO_PI * segment / segments;
                Vec3 point = centre.add(spoke.scale(Mth.cos(angle) * radius))
                        .add(other.scale(Mth.sin(angle) * radius));

                if (previous != null) {
                    ribbon(previous, point, red, green, blue, alpha, alpha, widthScale);
                }

                previous = point;
            }
        }

        private float width(Vec3 point, float scale) {
            return Math.max(MIN_RIBBON_HALF_WIDTH, (float) this.eye.distanceTo(point) * RIBBON_HALF_WIDTH_PER_BLOCK)
                    * scale;
        }
    }
}
