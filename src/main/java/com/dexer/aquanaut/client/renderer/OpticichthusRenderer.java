package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.OpticichthusModel;
import com.dexer.aquanaut.common.entity.OpticichthusEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Draws the Opticichthus and, on top of it, its lance.
 *
 * <p>
 * The beam is a bundle of additive, full-bright quads drawn in entity space so it can be built from
 * world-space points, but everything about it is kept deliberately blocky to match the rest of the
 * game: the texture is nearest-filtered 16px pixel art, the cross-section is a hard stair-step, the
 * length fade steps once per segment, sprites are quantised to whole texels and animated by swapping
 * frames rather than rotating, and the energy that runs up the beam is a string of chunky pixel
 * sparks rather than a smooth helix.
 *
 * <ul>
 * <li>a razor core ribbon that always faces the camera,</li>
 * <li>two crossed glow ribbons (a volumetric-looking cross-section from any angle),</li>
 * <li>two strands of pixel sparks corkscrewing around the axis,</li>
 * <li>a blocky two-frame star flare at the muzzle and at the impact point,</li>
 * <li>and, while charging, a strobing targeting line with a shrinking aperture flare.</li>
 * </ul>
 *
 * The textures carry the profile and the palette; the per-vertex colour carries the heat envelope, so
 * the lance blows out white at the muzzle and cools toward its far end.
 */
public class OpticichthusRenderer extends BaseFishRenderer<OpticichthusEntity> {
    private static final int RIBBON_SEGMENTS = 12;
    private static final int BEAM_TEXELS_ACROSS = 16;
    private static final int BEAM_TEXELS_ALONG = 64;
    /** Fastest scroll the beam may use, for hair-thin ribbons. */
    private static final double MAX_SCROLL = 1.2D;
    /** How fast the energy pattern travels down the beam, in blocks per tick. */
    private static final double SCROLL_BLOCKS_PER_TICK = 0.35D;
    private static final int SPARK_STEPS = 9;
    private static final int SPARK_STRANDS = 2;
    private static final int FULL_BRIGHT = 0x00F000F0;

    /** One block is 16 texels of the sprite; sprites and offsets snap to this so pixels stay square. */
    private static final float TEXEL = 1.0F / 16.0F;

    /** Beam half-widths. Each layer's full width is an exact multiple of the 16px cross-section. */
    private static final double CORE_HALF_WIDTH = 0.10D;
    private static final double GLOW_HALF_WIDTH = 0.22D;
    private static final double HAZE_HALF_WIDTH = 0.40D;

    /** Client-side cache for the charge-phase targeting raycast (one clip per tick, per entity). */
    private int sightTick = Integer.MIN_VALUE;
    private int sightEntity = -1;
    private double sightLength = OpticichthusEntity.BEAM_RANGE;

    public OpticichthusRenderer(EntityRendererProvider.Context context) {
        super(context, new OpticichthusModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void render(OpticichthusEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        int phase = entity.getPhase();
        if (phase != OpticichthusEntity.PHASE_CHARGING && phase != OpticichthusEntity.PHASE_FIRING) {
            return;
        }

        poseStack.pushPose();
        try {
            if (phase == OpticichthusEntity.PHASE_CHARGING) {
                this.renderCharge(entity, partialTick, poseStack, bufferSource);
            } else {
                this.renderLance(entity, partialTick, poseStack, bufferSource);
            }
        } finally {
            poseStack.popPose();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Firing
    // ---------------------------------------------------------------------------------------------

    private void renderLance(OpticichthusEntity entity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource) {
        double length = entity.getBeamLength();
        if (length <= 0.05D) {
            return;
        }

        float age = entity.tickCount + partialTick;
        float progress = entity.getBeamProgress() + partialTick / OpticichthusEntity.FIRING_TICKS;
        float envelope = LaserGeometry.envelope(progress);
        // Quantised flicker: the beam steps between a handful of brightness levels instead of easing.
        float flicker = 0.80F + 0.20F * quantise((float) Math.sin(age * 0.9F), 4);

        Vec3 base = entity.getPosition(partialTick);
        Vec3 origin = entity.getLensOrigin(partialTick).subtract(base);
        Vec3 direction = entity.getAimDirection();
        if (direction.lengthSqr() < 0.5D) {
            // Corrupt aim vector (never expected): skip rather than fill the buffer with NaNs.
            return;
        }
        Vec3 side = this.cameraFacingAxis(direction);
        Vec3 side2 = direction.cross(side).normalize();

        VertexConsumer buffer = bufferSource.getBuffer(LaserRenderTypes.beam());

        // Razor core: one camera-facing ribbon, white hot.
        this.ribbon(buffer, poseStack.last().pose(), origin, direction, side,
                CORE_HALF_WIDTH, length, 1.0F, 1.0F, 1.0F, 0.95F * envelope * flicker, age);

        // Crossed glow ribbons.
        float glow = 0.55F * envelope * flicker;
        float haze = 0.20F * envelope;
        for (Vec3 axis : new Vec3[] { side, side2 }) {
            this.ribbon(buffer, poseStack.last().pose(), origin, direction, axis,
                    GLOW_HALF_WIDTH, length, 0.74F, 0.95F, 1.0F, glow, age);
            this.ribbon(buffer, poseStack.last().pose(), origin, direction, axis,
                    HAZE_HALF_WIDTH, length, 0.44F, 0.64F, 1.0F, haze, age);
        }

        this.orbitSparks(poseStack, bufferSource, origin, direction, side, side2, length, age, envelope);

        // Muzzle and impact light.
        Vec3 end = origin.add(direction.scale(length));
        float discharge = LaserGeometry.clamp(1.0F - progress * 4.0F);
        this.flare(poseStack, bufferSource, origin, snap(0.55F + 0.35F * envelope),
                frameIndex(age, 0.0F), 0.86F, 0.97F, 1.0F, 0.85F * envelope);
        if (discharge > 0.02F) {
            this.flare(poseStack, bufferSource, origin, snap(2.20F * discharge),
                    frameIndex(age, 0.5F), 1.0F, 1.0F, 1.0F, 0.7F * discharge);
        }
        this.flare(poseStack, bufferSource, end, snap(1.80F),
                frameIndex(age, 0.25F), 0.70F, 0.90F, 1.0F, 0.30F * envelope);
        this.flare(poseStack, bufferSource, end, snap(entity.isBeamContact() ? 1.30F : 0.90F),
                frameIndex(age, 0.75F), 1.0F, 0.98F, 0.95F,
                (entity.isBeamContact() ? 0.95F : 0.60F) * envelope);
    }

    /**
     * Two strands of chunky pixel sparks corkscrewing around the lance. Sparks on the camera's side
     * of the beam are drawn brighter, which is what makes the rotation readable in a still frame.
     */
    private void orbitSparks(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 origin, Vec3 direction,
            Vec3 side, Vec3 side2, double length, float age, float envelope) {
        for (int strand = 0; strand < SPARK_STRANDS; strand++) {
            double phase = Math.PI * strand;
            for (int i = 0; i < SPARK_STEPS; i++) {
                double t = (i + 0.5D) / SPARK_STEPS;
                Vec3 radial = LaserGeometry.helixOffset(side, side2, t, phase, age);
                if (radial.lengthSqr() < 1.0E-8D) {
                    continue;
                }
                Vec3 centre = origin.add(direction.scale(length * t)).add(radial);
                float facing = (float) Math.max(0.0D, radial.normalize().dot(side));
                float alpha = 0.60F * envelope * (0.25F + 0.75F * facing) * (0.45F + 0.55F * (float) t);
                float size = snap(t < 0.5D ? 0.12F : 0.16F);
                this.flare(poseStack, bufferSource, centre, size,
                        frameIndex(age, strand * 0.5F + i * 0.13F), 0.60F, 1.0F, 1.0F, alpha);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Charging
    // ---------------------------------------------------------------------------------------------

    private void renderCharge(OpticichthusEntity entity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource) {
        float age = entity.tickCount + partialTick;
        float progress = entity.getChargeProgress();
        // Hard strobe on the sight line: a ranging laser, not a solid beam.
        float strobe = (entity.tickCount / 2) % 2 == 0 ? 1.0F : 0.30F;
        float heat = progress * progress;
        // Cyan while ranging, warning red as the shot becomes imminent.
        float red = 0.45F + 0.55F * heat;
        float green = 0.92F - 0.58F * heat;
        float blue = 1.0F - 0.72F * heat;

        Vec3 base = entity.getPosition(partialTick);
        Vec3 origin = entity.getLensOrigin(partialTick).subtract(base);
        Vec3 direction = entity.getAimDirection();
        if (direction.lengthSqr() < 0.5D) {
            return;
        }
        Vec3 side = this.cameraFacingAxis(direction);

        VertexConsumer buffer = bufferSource.getBuffer(LaserRenderTypes.beam());

        // Targeting line, stopping on whatever rock is in the way.
        double sight = this.sightLength(entity, direction);
        this.ribbon(buffer, poseStack.last().pose(), origin, direction, side,
                0.05D, sight, red, green, blue, (0.10F + 0.55F * progress) * strobe, age);

        // Aperture: a wide ring closing down while a hot core grows inside it.
        this.flare(poseStack, bufferSource, origin, snap(1.30F - 0.95F * progress),
                frameIndex(age, 0.0F), 0.55F, 0.88F, 1.0F, (0.22F + 0.28F * progress) * strobe);
        this.flare(poseStack, bufferSource, origin, snap(0.08F + 0.34F * heat),
                frameIndex(age, 0.5F), red, green, blue, 0.35F + 0.55F * progress);

        // Reticle where the lance will land.
        if (sight > 2.0D) {
            Vec3 end = origin.add(direction.scale(sight));
            this.flare(poseStack, bufferSource, end, snap(0.10F + 0.24F * progress),
                    frameIndex(age, 0.25F), red, green, blue, (0.25F + 0.5F * progress) * strobe);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Primitives
    // ---------------------------------------------------------------------------------------------

    /**
     * One camera-facing ribbon: two quads per segment so the texture's stair-step cross-section shows
     * as a banded beam. The length fade is held constant across a segment, which bands the beam into
     * {@link #RIBBON_SEGMENTS} hard steps instead of a smooth gradient.
     */
    private void ribbon(VertexConsumer buffer, Matrix4f matrix, Vec3 origin, Vec3 direction, Vec3 side,
            double halfWidth, double length, float red, float green, float blue, float alpha,
            float age) {
        if (alpha <= 0.004F || length <= 0.02D) {
            return;
        }
        double vScale = scrollScale(halfWidth);
        float phase = (float) (-age * SCROLL_BLOCKS_PER_TICK * vScale);
        for (int i = 0; i < RIBBON_SEGMENTS; i++) {
            double t0 = (double) i / RIBBON_SEGMENTS;
            double t1 = (double) (i + 1) / RIBBON_SEGMENTS;
            double s0 = length * t0;
            double s1 = length * t1;

            Vec3 axis0 = origin.add(direction.scale(s0));
            Vec3 axis1 = origin.add(direction.scale(s1));
            Vec3 left0 = axis0.subtract(side.scale(halfWidth));
            Vec3 left1 = axis1.subtract(side.scale(halfWidth));
            Vec3 right0 = axis0.add(side.scale(halfWidth));
            Vec3 right1 = axis1.add(side.scale(halfWidth));

            // One alpha per segment: chunky steps along the beam, not a gradient.
            float step = alpha * LaserGeometry.fade((t0 + t1) * 0.5D);
            // Every layer's energy travels away from the lens at the same world speed; because each
            // layer repeats at a different size, that also gives the beam natural parallax.
            float v0 = (float) (s0 * vScale) + phase;
            float v1 = (float) (s1 * vScale) + phase;

            vertex(buffer, matrix, left0, 0.0F, v0, red, green, blue, step);
            vertex(buffer, matrix, left1, 0.0F, v1, red, green, blue, step);
            vertex(buffer, matrix, axis1, 0.5F, v1, red, green, blue, step);
            vertex(buffer, matrix, axis0, 0.5F, v0, red, green, blue, step);

            vertex(buffer, matrix, axis0, 0.5F, v0, red, green, blue, step);
            vertex(buffer, matrix, axis1, 0.5F, v1, red, green, blue, step);
            vertex(buffer, matrix, right1, 1.0F, v1, red, green, blue, step);
            vertex(buffer, matrix, right0, 1.0F, v0, red, green, blue, step);
        }
    }

    /**
     * Camera-facing billboard sprite (muzzle flash, aperture ring, impact burst, orbiting spark).
     *
     * <p>
     * The size snaps to whole texels and animation is done the way vanilla does it — by swapping
     * between the sprite's two frames — because these sprites are symmetric enough that spinning them
     * would look identical. Nothing is ever rotated, so every pixel on screen stays a square.
     *
     * @param frame 0 for the plus burst, 1 for the same burst turned 45 degrees
     */
    private void flare(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 centre, float size, int frame,
            float red, float green, float blue, float alpha) {
        if (alpha <= 0.004F || size <= 0.0F) {
            return;
        }
        float v0 = frame * 0.5F;
        float v1 = v0 + 0.5F;
        poseStack.pushPose();
        try {
            poseStack.translate(centre.x, centre.y, centre.z);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            Matrix4f matrix = poseStack.last().pose();
            VertexConsumer buffer = bufferSource.getBuffer(LaserRenderTypes.flare());
            vertex(buffer, matrix, new Vec3(-size, -size, 0.0D), 0.0F, v1, red, green, blue, alpha);
            vertex(buffer, matrix, new Vec3(size, -size, 0.0D), 1.0F, v1, red, green, blue, alpha);
            vertex(buffer, matrix, new Vec3(size, size, 0.0D), 1.0F, v0, red, green, blue, alpha);
            vertex(buffer, matrix, new Vec3(-size, size, 0.0D), 0.0F, v0, red, green, blue, alpha);
        } finally {
            poseStack.popPose();
        }
    }

    /** Five-frame-per-second pixel animation: a frame lasts four ticks, like vanilla sprite sheets. */
    private static int frameIndex(float age, float offset) {
        return ((int) Math.floor(age / 4.0F + offset)) & 1;
    }

    /**
     * The axis to widen the beam along so a flat ribbon still reads as a cylinder: perpendicular to
     * both the beam and the view direction.
     */
    private Vec3 cameraFacingAxis(Vec3 direction) {
        org.joml.Vector3f look = this.entityRenderDispatcher.camera.getLookVector();
        return LaserGeometry.cameraFacingAxis(direction, new Vec3(look.x, look.y, look.z));
    }

    private double sightLength(OpticichthusEntity entity, Vec3 direction) {
        if (this.sightTick != entity.tickCount || this.sightEntity != entity.getId()) {
            this.sightTick = entity.tickCount;
            this.sightEntity = entity.getId();
            Vec3 lens = entity.getLensOrigin(1.0F);
            Vec3 reach = lens.add(direction.scale(OpticichthusEntity.BEAM_RANGE));
            BlockHitResult hit = entity.level().clip(new ClipContext(lens, reach,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            this.sightLength = hit.getType() == HitResult.Type.MISS
                    ? OpticichthusEntity.BEAM_RANGE
                    : lens.distanceTo(hit.getLocation());
        }
        return this.sightLength;
    }

    /**
     * Repeats per block that keep the beam sprite's pixels square on a ribbon this wide: the texture
     * is {@link #BEAM_TEXELS_ACROSS} x {@link #BEAM_TEXELS_ALONG}, so the repeat has to be sized from
     * the ribbon's width. Clamped for very thin ribbons, where square pixels would repeat so fast
     * that the pattern would alias into noise.
     */
    private static double scrollScale(double halfWidth) {
        double repeat = 2.0D * halfWidth * BEAM_TEXELS_ALONG / BEAM_TEXELS_ACROSS;
        return Math.min(MAX_SCROLL, 1.0D / repeat);
    }

    /** Snaps a sprite half-size to whole texels so its pixels stay square on screen. */
    private static float snap(float size) {
        return Math.max(TEXEL, Math.round(size / TEXEL) * TEXEL);
    }

    /** Snaps a wave to {@code steps} levels, for animation that jumps instead of easing. */
    private static float quantise(float value, int steps) {
        float scaled = (value * 0.5F + 0.5F) * steps;
        return Math.round(scaled) / steps * 2.0F - 1.0F;
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Vec3 point, float u, float v,
            float red, float green, float blue, float alpha) {
        // The format packs each channel into a byte, so an out-of-range value would wrap and darken
        // the beam instead of saturating it.
        buffer.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .setColor(LaserGeometry.clamp(red), LaserGeometry.clamp(green),
                        LaserGeometry.clamp(blue), LaserGeometry.clamp(alpha))
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }
}
