package com.dexer.aquanaut.client.drone;

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.mixin.CameraAccessor;
import com.dexer.aquanaut.common.entity.SubmarineDroneEntity;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The drone's camera feed: the game renders one frame from the pod's position, and the HUD lays a
 * small copy of it over the player's own view.
 *
 * <h3>The picture is the game's, not this class's</h3>
 * Everything here used to be an imitation of a frame: a camera of its own, its own projection and cull
 * frustum, its own target, and a call into {@code LevelRenderer.renderLevel}. Each version lost
 * something the real frame sets up — entities that never appeared, glow masks that came out wrong,
 * terrain framed for the wrong buffer, a picture with a shader pack smeared across it. A frame is not a
 * function. The renderer, the dispatchers, the occlusion data and every mod hooked into the world
 * render all hold state for <em>the</em> frame, and an imitation of a frame is not one.
 *
 * <p>
 * So the game renders it. The camera entity is swapped for the drone and
 * {@link GameRenderer#renderLevel} is called — the same call the player's own frame makes, doing
 * everything it does: the camera, the projection, the pick, the cull frustum, the fog, the lightmap,
 * the render dispatchers, the shadow and composite phases. What comes out is a complete frame from the
 * pod's position, and this class only copies it, crops it and puts the operator's state back. Nothing
 * about the picture is assembled here, so there is nothing here to get subtly wrong.
 *
 * <p>
 * It also settles the small things for free. Vanilla only draws the block outline when the camera
 * entity is a player ({@code shouldRenderBlockOutline}), and this camera is the drone, so the feed has
 * no crosshair highlight in it. The hand is drawn after the point the copy is taken, so it is not in
 * the picture either.
 *
 * <h3>When the copy is taken</h3>
 * The copy happens at {@link RenderLevelStageEvent.Stage#AFTER_LEVEL}, dispatched by {@code renderLevel}
 * once the world is complete — terrain, entities, block entities, particles, weather — and before that
 * method draws the operator's own hand over it. It is both the last moment the feed's frame exists and
 * the only moment it is the drone's view alone.
 *
 * <h3>Why it runs before the frame, not inside it</h3>
 * Rendering the pod's frame from inside the player's level render — at that same stage, on the player's
 * frame — is not safe: {@code renderLevel} flushes the shared geometry buffer, re-points the global fog
 * and the render dispatchers, and under Fabulous graphics finishes by compositing weather and clouds
 * into the frame's target. The operator's own render would inherit all of it. Rendering first, and
 * letting the game draw the operator's real frame afterwards from nothing, means nothing the feed
 * leaves behind can survive into their view.
 *
 * <h3>One renderer has to be asked to stand aside</h3>
 * A shader pack owns the frame rather than decorating it: a pipeline with deferred passes and
 * accumulated history, none of which has any notion of a second camera. Driving it here would make the
 * operator's whole screen flicker between the two views and smear everything temporal. The pack is
 * therefore stood down for the capture ({@link ShaderStanddown}) and the world is rendered plainly —
 * which is what a downlink shows anyway. That is the only renderer-specific piece of this feature, and
 * it is deliberately on its own; if it cannot be arranged the feed simply does not render.
 *
 * <h3>The buffer</h3>
 * A fixed 256×144, never resized with the window: a centred 16:9 crop of the frame, scaled into a fixed
 * texture. A downlink, not a mirror — a window resize cannot invalidate it mid-frame, and a small image
 * scaled up to the panel is exactly what a video feed looks like.
 *
 * <h3>The downlink's clock</h3>
 * A capture costs a whole frame of world rendering, so it does not run on every frame. It is given a
 * slice of the frame instead: the next capture is due once the last one has been paid for within
 * {@link #FRAME_BUDGET}, which works out at a steady handful of updates a second. In between, the panel
 * shows the last picture it was given. A feed that lags a little is a feed; a second world render on
 * every frame is a tax on everything else the operator is doing.
 *
 * <h3>The lens</h3>
 * The view is a wide angle — {@link #FEED_FOV} — because the panel is small and a feed is for flying
 * by, not for looking through. It is the pod's lens rather than a copy of the operator's own field of
 * view, which vanilla narrows underwater and which describes their eyes rather than the drone's. Their
 * field of view and their camera setting are pinned for the capture and put back before their own frame
 * draws.
 *
 * <p>
 * The height the picture is taken from is the one thing that is the drone's rather than theirs: the
 * camera's eye height, which is otherwise a running average of the operator's own eyes, is pinned to
 * the middle of the pod's nose sensor for the capture — see {@link #capture}.
 *
 * <h3>What the pod can see</h3>
 * The abyss fog belongs to the observer, so the feed does not inherit it: the pod keeps the water's own
 * colour and visibility where the operator's eyes would be closed down to a few blocks by pressure
 * ({@code ClientFogEvents}). That is the point of a camera you can send ahead, and it also keeps the
 * operator's drowning clock out of the picture — a machine has no air to run out of, so a feed that
 * took part in that clock would hold the darkness at nothing for everyone.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, value = Dist.CLIENT)
public final class DroneFeedRenderer {

    /** Downlink resolution. */
    public static final int FEED_WIDTH = 256;
    public static final int FEED_HEIGHT = 144;

    /**
     * The sensor pod's own lens: the vertical field of view it renders, in degrees.
     *
     * <p>
     * Deliberately a little wider than the operator's — but not much. What makes the feed worth
     * watching is that it sees <em>past</em> the abyss rather than through more of it (see
     * {@code ClientFogEvents}), so the lens only has to frame the picture, and an extreme one distorts
     * the edges of a viewport this small for no gain. The operator's own setting still raises it, so
     * widening their view can never leave the feed as the narrowest thing on the screen.
     */
    private static final double FEED_FOV = 80.0D;

    /**
     * How opaque the overlay is: nearly all of it, so the viewport reads as a screen the way a monitor
     * does, rather than as a tint over the world the operator is standing in.
     */
    private static final int FEED_ALPHA = 230;

    // ── the camera's look ─────────────────────────────────────────────────────
    /** Cool sensor grade, laid over the picture so it reads as a lens rather than a window. */
    private static final int DAY_GRADE = 0x1C0A2A38;

    /** One dark line in three, the cheapest honest way to say "video". */
    private static final int SCANLINE = 0x14000000;
    private static final int SCANLINE_STEP = 3;

    /** Corner falloff, in rings: the outermost is darkest, so the edge fades rather than steps. */
    private static final int[][] VIGNETTE_RINGS = { { 2, 0x30000000 }, { 4, 0x20000000 }, { 7, 0x14000000 } };

    /** A one-pixel highlight along the top and left of the viewport, as if it were a lit screen. */
    private static final int BEZEL = 0x387BE9FF;

    /** Where the one-shot readback samples the buffer, as percentages of its size. */
    private static final int[][] PROBE_POINTS = { { 50, 50 }, { 12, 12 }, { 88, 88 }, { 50, 10 }, { 50, 90 } };

    /** What the one-shot readback found in the buffer. */
    public enum Verdict {
        /** Nothing rendered yet. */
        UNKNOWN,
        /** There is a picture in there. */
        PICTURE,
        /** The buffer came back black: the render produced nothing usable. */
        BLACK
    }

    @Nullable
    private static RenderTarget target;

    /** Set once the render pipeline has thrown, so a broken driver is not hit every frame. */
    private static boolean failed;

    /** Re-entrancy guard, kept because a second render is exactly the kind of thing that can recurse. */
    private static boolean insideFeed;

    // ── the downlink's own clock ──────────────────────────────────────────────
    /** The share of a frame the feed may spend, before it starts skipping updates. */
    private static final double FRAME_BUDGET = 0.1D;
    /** The slowest the feed is ever allowed to fall to, in milliseconds. */
    private static final long MAX_INTERVAL_MS = 250L;

    /** When the next update is due, on {@link System#nanoTime}. Zero means "now". */
    private static long dueNanos;
    /** Smoothed cost of one pass, in nanos, so the budget can be honoured without measuring every frame. */
    private static long costNanos;

    /** How often the buffer is sampled again, in frames. One readback per second is plenty. */
    private static final int PROBE_INTERVAL = 40;

    private static int probeCountdown;
    private static boolean probedOnce;
    private static Verdict verdict = Verdict.UNKNOWN;

    private DroneFeedRenderer() {
    }

    /** Whether the feed was rendered and can be drawn. */
    public static boolean isAvailable() {
        return !failed && target != null;
    }

    /** Whether the feed is unavailable, so the HUD can say so rather than draw a stale frame. */
    public static boolean hasFailed() {
        return failed;
    }

    /** What the one-shot readback found. */
    public static Verdict verdict() {
        return verdict;
    }

    /** Clears a previous failure, so entering a new world gives the pipeline another chance. */
    public static void reset() {
        failed = false;
        probedOnce = false;
        probeCountdown = 0;
        dueNanos = 0L;
        costNanos = 0L;
        verdict = Verdict.UNKNOWN;
    }

    // ------------------------------------------------------------------
    // the second render
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        if (insideFeed || failed) {
            return;
        }
        SubmarineDroneEntity drone = ClientDroneState.drone();
        if (drone == null || !ClientDroneState.isFlying()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.options.hideGui || drone.isRemoved()) {
            return;
        }
        if (System.nanoTime() < dueNanos) {
            // Not due yet: the last picture is still on the panel, which is what a downlink does.
            return;
        }

        insideFeed = true;
        long started = System.nanoTime();
        try {
            capture(event.getPartialTick(), minecraft, drone);
        } catch (Exception | LinkageError failure) {
            // A second render of the world is the one part of this feature a chunk renderer or a
            // shader pack can refuse to cooperate with. Losing the feed is recoverable; losing the
            // frame, or the game, is not.
            failed = true;
            Aquanaut.LOGGER.error("Disabling the submarine drone camera feed after a render failure", failure);
        } finally {
            insideFeed = false;
            scheduleNextUpdate(System.nanoTime() - started);
        }
    }

    /**
     * Sets when the next update may run: after this pass has been paid for.
     *
     * <p>
     * The pass is a full render of the world at the frame's own size, so it cannot run on every
     * frame without halving the frame rate, and with a shader pack it costs several times that. The
     * downlink is given a slice of the frame instead of a share of the frame rate: the next update is
     * due once the last one is amortised, which leaves a cheap pipeline nearly frame-rate and a heavy
     * one at a handful of updates a second. That is a feed, not a mirror — it is the cheapest place to
     * trade, and the HUD keeps showing the last picture it was given.
     */
    private static void scheduleNextUpdate(long cost) {
        costNanos = costNanos == 0L ? cost : (costNanos * 3L + cost) / 4L;
        long interval = (long) (costNanos / FRAME_BUDGET);
        dueNanos = System.nanoTime() + Math.min(interval, MAX_INTERVAL_MS * 1000_000L);
    }

    /**
     * Takes a picture of what the pod can see, through the game's own camera.
     *
     * <h3>Why the game is asked to render, rather than a copy of it</h3>
     * The first four attempts at this feature hand-assembled a second world render: a camera of its
     * own, its own projection, its own cull frustum, its own target, and a call into
     * {@code LevelRenderer.renderLevel}. Every one of them lost something the real frame passes set up —
     * entities that never appeared, glow masks that came out wrong, terrain framed for the wrong
     * buffer — because a frame is not a function. The renderer, the dispatchers, the occlusion data and
     * every mod hooked into the world render all keep state for <em>the</em> frame, and an imitation of
     * a frame is not one.
     *
     * <p>
     * So the game renders this one. The camera entity is swapped for the drone and
     * {@link GameRenderer#renderLevel} is called — the very call the player's own frame makes, doing
     * everything it does: the camera and its projection, the pick, the cull frustum, fog, the lightmap,
     * the dispatchers, the shadow and composite phases. What comes out is a complete frame from the
     * pod's position, and the only thing the feed has to do with it is copy it out.
     *
     * <h3>What the picture is taken from, and when</h3>
     * The frame's own buffer, because that is where every render pipeline agrees to draw, and at the
     * frame's own size, because that is the size they all agree on. The downlink is a centred 16:9 crop
     * of it, scaled into 256×144.
     *
     * <p>
     * The copy happens at {@link RenderLevelStageEvent.Stage#AFTER_LEVEL} — after the world is complete,
     * and before {@code renderLevel} draws the operator's own hand over it. Taking it at the end of the
     * call instead would put the player's arm in the drone's feed.
     *
     * <h3>And the pack?</h3>
     * A shader pack owns the frame rather than decorating it, so it is stood down for the duration
     * ({@link ShaderStanddown}) — otherwise this render would be a second camera through state that
     * describes one, and the player's whole screen would flicker and smear. What is left is the world
     * rendered plainly, which is what a downlink shows anyway.
     */
    private static void capture(DeltaTracker delta, Minecraft minecraft, SubmarineDroneEntity drone) {
        // A renderer that owns the frame must stand aside. If it cannot be arranged, the feed does not
        // run at all: a feed is worth having, the operator's screen is not worth losing.
        if (!ShaderStanddown.quiet()) {
            failed = true;
            Aquanaut.LOGGER.error(
                    "Disabling the submarine drone camera feed: a shader pack owns the world render and could not be stood down for the pass",
                    ShaderStanddown.failure());
            return;
        }

        GameRenderer gameRenderer = minecraft.gameRenderer;
        Entity previousCamera = minecraft.getCameraEntity();
        CameraType previousCameraType = minecraft.options.getCameraType();
        int previousFov = minecraft.options.fov().get();

        // The camera keeps its eye height as a running average dragged toward whichever entity it was
        // last ticked against — the operator, 1.62 blocks up — and a second render for the same frame
        // does not tick it. Left alone, the pod would fly its whole sortie from a viewpoint more than a
        // block above its own hull; here it is pinned to the pod's own sensor for the capture and put
        // back before the operator's frame draws.
        CameraAccessor camera = (CameraAccessor) (Object) gameRenderer.getMainCamera();
        float previousEyeHeight = camera.aquanaut$getEyeHeight();
        float previousEyeHeightOld = camera.aquanaut$getEyeHeightOld();

        capturing = true;
        try {
            // The pod's own eyes: pointed at the drone, through a first-person camera, on its own lens,
            // at the height of its own sensor. The operator's third-person setting and field of view are
            // theirs, and are put back below.
            minecraft.setCameraEntity(drone);
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            minecraft.options.fov().set((int) Math.max(FEED_FOV, previousFov));
            camera.aquanaut$setEyeHeight(drone.getEyeHeight());
            camera.aquanaut$setEyeHeightOld(drone.getEyeHeight());

            gameRenderer.renderLevel(delta);
        } finally {
            capturing = false;
            // Whatever happened, the frame's target is bound again: a level render draws into the bound
            // buffer, and the frame this one interrupted still has work to do in it.
            minecraft.getMainRenderTarget().bindWrite(true);
            camera.aquanaut$setEyeHeight(previousEyeHeight);
            camera.aquanaut$setEyeHeightOld(previousEyeHeightOld);
            minecraft.options.fov().set(previousFov);
            minecraft.options.setCameraType(previousCameraType);
            minecraft.setCameraEntity(previousCamera);
            ShaderStanddown.resume();
        }
    }

    /**
     * Whether the render in flight is the feed's. Written by {@link #capture}, read by
     * {@link #onRenderLevelStage}: the same stage fires for the operator's own render, which is not a
     * picture to take.
     */
    private static boolean capturing;

    /**
     * Takes the picture, at the one moment it is complete and still clean.
     *
     * <p>
     * {@code renderLevel} dispatches this stage after the world — terrain, entities, block entities,
     * particles, weather, the lot — and before it draws the operator's hand over the top. Copying here
     * is therefore both the last moment the feed's frame exists and the only moment it is the drone's
     * view alone.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!capturing || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        RenderTarget feed = target();

        // The copy leaves the downlink bound, because the two steps after it work on the downlink: the
        // alpha fix clears the bound buffer, and the probe reads it.
        takeDownlink(feed, main);
        solidifyAlpha();
        probe(feed, ClientDroneState.drone(), minecraft);

        // And then the frame's own target goes straight back. This runs in the middle of a level
        // render, and a level render draws into whatever is bound: the game binds the frame's target
        // after this call, not before it. Leaving the downlink bound here is how one frame's view ends
        // up drawn into the other's buffer — the operator's own render lands in the 256×144 downlink
        // while the screen keeps showing the picture that was just taken, and the two views flicker
        // across the whole screen.
        main.bindWrite(true);
    }

    /**
     * Copies the downlink's picture out of the frame's buffer.
     *
     * <p>
     * The largest centred rectangle of the feed's shape, scaled into 256×144. Centred because the
     * pod's view is already framed by its own projection, so the middle of the frame is the middle of
     * the picture; a corner would be a crop of one side of it.
     */
    private static void takeDownlink(RenderTarget feed, RenderTarget main) {
        int cropWidth = main.height * FEED_WIDTH / FEED_HEIGHT;
        int cropHeight = main.height;
        if (cropWidth > main.width) {
            cropWidth = main.width;
            cropHeight = main.width * FEED_HEIGHT / FEED_WIDTH;
        }
        int cropX = (main.width - cropWidth) / 2;
        int cropY = (main.height - cropHeight) / 2;

        feed.bindWrite(true);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBlitFrameBuffer(cropX, cropY, cropX + cropWidth, cropY + cropHeight,
                0, 0, FEED_WIDTH, FEED_HEIGHT, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        feed.bindWrite(false);
    }

    /**
     * Gives the buffer a solid alpha channel without touching one colour value.
     *
     * <p>
     * The downlink's buffer is the bound target when this runs, because the whole pass drew into it.
     * The level render clears with the fog colour, whose alpha is zero, and the copy carries that
     * through, so anywhere nothing was drawn ends up transparent. The feed is composited with a vertex
     * alpha for the translucency the overlay needs, and texture alpha multiplies into that — a
     * zero-alpha sky would punch a hole straight through the world behind it. Clearing only the alpha
     * channel fixes it exactly.
     */
    private static void solidifyAlpha() {
        RenderSystem.colorMask(false, false, false, true);
        RenderSystem.clearColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.clear(16384, Minecraft.ON_OSX);
        RenderSystem.colorMask(true, true, true, true);
    }

    /**
     * Samples the viewport and says what is actually in it.
     *
     * <p>
     * A feed that shows nothing has several very different causes — nothing rendered, something
     * rendered flat and dark enough to read as nothing, or something rendered that the overlay then
     * failed to present — and they look identical from the outside. A readback separates them, and the
     * verdict is also shown in the window so it does not take a log dive to see which one it is.
     *
     * <p>
     * Re-sampled periodically rather than once, because a drone that is momentarily buried in rock
     * should not latch the panel into "no signal" for the rest of the session.
     */
    private static void probe(RenderTarget feed, SubmarineDroneEntity drone, Minecraft minecraft) {
        if (probeCountdown > 0) {
            probeCountdown--;
            return;
        }
        probeCountdown = PROBE_INTERVAL;
        try {
            feed.bindWrite(false);
            ByteBuffer pixel = ByteBuffer.allocateDirect(4);
            StringBuilder samples = new StringBuilder();
            int brightest = 0;
            for (int[] at : PROBE_POINTS) {
                int x = Mth.clamp(at[0] * feed.width / 100, 0, feed.width - 1);
                int y = Mth.clamp(at[1] * feed.height / 100, 0, feed.height - 1);
                pixel.clear();
                GlStateManager._readPixels(x, y, 1, 1, 6408, 5121, pixel);
                int r = pixel.get(0) & 0xFF;
                int g = pixel.get(1) & 0xFF;
                int b = pixel.get(2) & 0xFF;
                int a = pixel.get(3) & 0xFF;
                brightest = Math.max(brightest, Math.max(r, Math.max(g, b)));
                samples.append(x).append(',').append(y).append("=(")
                        .append(r).append(' ').append(g).append(' ').append(b).append(' ').append(a)
                        .append(") ");
            }
            verdict = brightest < 8 ? Verdict.BLACK : Verdict.PICTURE;
            if (!probedOnce) {
                probedOnce = true;
                Aquanaut.LOGGER.warn(
                        "Drone feed probe: {} | fbo={} tex={} {}x{} | drone {} at ({}, {}, {}) inWater={} | samples {}",
                        verdict, feed.frameBufferId, feed.getColorTextureId(), feed.width, feed.height,
                        drone.getUUID(), (int) drone.getX(), (int) drone.getY(), (int) drone.getZ(),
                        drone.isInWater(), samples.toString().trim());
            }
        } catch (Exception | LinkageError failure) {
            if (!probedOnce) {
                probedOnce = true;
                Aquanaut.LOGGER.warn("Drone feed probe failed", failure);
            }
        } finally {
            minecraft.getMainRenderTarget().bindWrite(false);
        }
    }

    private static RenderTarget target() {
        if (target == null) {
            target = new TextureTarget(FEED_WIDTH, FEED_HEIGHT, true, false);
            target.setFilterMode(9729);
        }
        return target;
    }

    // ------------------------------------------------------------------
    // presentation
    // ------------------------------------------------------------------

    /**
     * Draws the feed into a rectangle as a translucent overlay, with the camera's look on top.
     *
     * <p>
     * The blit is issued by hand rather than through the texture manager: the feed's pixels live in
     * the render target's own colour attachment, and handing that id to a texture the resource manager
     * believes it owns is how a resource reload ends up deleting the buffer out from under the frame.
     *
     * <p>
     * It goes through the position-tex-colour shader so the whole viewport can be laid down at one
     * constant opacity. The texture's own alpha has already been forced solid, so the only thing
     * deciding how much of the world shows through is {@link #FEED_ALPHA}.
     *
     * <p>
     * The texture coordinates run the other way up from every other texture the GUI draws. A resource
     * pack's texture is handed out with its first row at the top; a render target's first row is its
     * bottom one — the same inversion that leaves a screenshot of one upside down until it is
     * flipped. Hence the v values below, and the picture would be drawn inverted without them.
     */
    public static void draw(GuiGraphics graphics, int x, int y, int width, int height) {
        if (target == null) {
            return;
        }

        // Everything already queued is drawn first, so the feed cannot end up in front of it.
        graphics.flush();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());
        // The shader multiplies the texture by the vertex colour and the global colour modulator, and
        // the modulator is shared state that earlier HUD drawing may have left part-transparent. It
        // has to be identity here or the whole viewport is silently faded out by someone else's alpha.
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // Depth belongs to the world behind the HUD, and the feed is an overlay on top of all of it.
        RenderSystem.disableDepthTest();

        Matrix4f pose = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        vertex(buffer, pose, x, y + height, 0.0F, 0.0F);
        vertex(buffer, pose, x + width, y + height, 1.0F, 0.0F);
        vertex(buffer, pose, x + width, y, 1.0F, 1.0F);
        vertex(buffer, pose, x, y, 0.0F, 1.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.enableDepthTest();
        drawLook(graphics, x, y, width, height);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f pose, int x, int y, float u, float v) {
        buffer.addVertex(pose, (float) x, (float) y, 0.0F)
                .setUv(u, v)
                .setColor(255, 255, 255, FEED_ALPHA);
    }

    /**
     * The camera's look, confined to the viewport: a sensor grade, one dark line in three, corner
     * falloff and a lit bezel. Subtle on purpose — this is meant to read as a lens and a downlink,
     * not as a filter laid over the player's screen, which is why none of it escapes the rectangle.
     */
    private static void drawLook(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, DAY_GRADE);

        for (int line = y + 2; line < y + height; line += SCANLINE_STEP) {
            graphics.fill(x, line, x + width, line + 1, SCANLINE);
        }

        for (int[] ring : VIGNETTE_RINGS) {
            int depth = ring[0];
            int colour = ring[1];
            graphics.fill(x, y, x + width, y + depth, colour);
            graphics.fill(x, y + height - depth, x + width, y + height, colour);
            graphics.fill(x, y, x + depth, y + height, colour);
            graphics.fill(x + width - depth, y, x + width, y + height, colour);
        }

        // A lit top-left edge, so the viewport reads as a screen rather than a hole.
        graphics.fill(x, y, x + width, y + 1, BEZEL);
        graphics.fill(x, y, x + 1, y + height, BEZEL);
    }
}