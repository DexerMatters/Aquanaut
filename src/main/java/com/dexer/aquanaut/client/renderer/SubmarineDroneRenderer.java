package com.dexer.aquanaut.client.renderer;

import javax.annotation.Nullable;

import com.dexer.aquanaut.client.model.SubmarineDroneModel;
import com.dexer.aquanaut.common.entity.SubmarineDroneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Draws the submarine drone, and its lamps through its glowmask: the lens, the nose sensor, the
 * floodlight and the sensor-pod scanner strip.
 *
 * <p>
 * There are two skins and therefore two masks, chosen by {@link SubmarineDroneModel} from the
 * drone's control state: {@code submarine_drone_glowmask.png} lights a live drone, while
 * {@code submarine_drone_deactivated_glowmask.png} is empty and leaves a powered-down one dark.
 * GeckoLib's glow layer takes care of the pairing, since it derives the mask path from whichever
 * base texture the model returns.
 *
 * <p>
 * The hull is opaque stainless, so this cuts rather than blends — the only transparency in either
 * texture is the gutter around the atlas islands, and the impeller is a zero-thickness plate whose
 * transparent cells have to be cut rather than sorted.
 *
 * <p>
 * Unlike the fish renderer it is not built on, this one has no flexible-body behaviour to get out of
 * the way of: a rigid hull trims in a dive, so the model's pitch is always applied, pivoting about
 * the middle of the hitbox.
 */
public final class SubmarineDroneRenderer extends GeoEntityRenderer<SubmarineDroneEntity> {

    /** Fraction of the hull's height the model pivots about when the nose rises or falls. */
    private static final double PITCH_PIVOT_FRACTION = 0.5D;

    /** Gap between the top of the hull and its tag. */
    private static final float TAG_HEIGHT_OFFSET = 0.5F;

    public SubmarineDroneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SubmarineDroneModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    /**
     * Turns the hull onto its heading, then trims its nose.
     *
     * <p>
     * The heading has to be handed to GeckoLib explicitly. {@code GeoEntityRenderer} reads a model's
     * yaw from the living-body fields — {@code yBodyRot}/{@code yHeadRot} — and a machine has none, so
     * for anything that is not a {@code LivingEntity} it passes a yaw of zero and the model is drawn
     * at one fixed heading no matter which way the entity is actually pointing. On a steered hull that
     * reads as the drone sliding sideways.
     *
     * <p>
     * A drone flies by a single heading and has no separate body or head, so its own view yaw
     * <em>is</em> the model's yaw.
     */
    @Override
    protected void applyRotations(SubmarineDroneEntity animatable, PoseStack poseStack, float ageInTicks,
            float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, animatable.getViewYRot(partialTick),
                partialTick, nativeScale);

        float pitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());
        if (Math.abs(pitch) < 0.01F) {
            return;
        }

        double pivotY = animatable.getBbHeight() * PITCH_PIVOT_FRACTION / nativeScale;
        poseStack.translate(0.0D, pivotY, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.translate(0.0D, -pivotY, 0.0D);
    }

    @Override
    public void render(SubmarineDroneEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        TagLabelRenderer.render(entity, TAG_HEIGHT_OFFSET, this.entityRenderDispatcher, getFont(),
                poseStack, bufferSource, packedLight);
    }

    @Override
    public @Nullable RenderType getRenderType(SubmarineDroneEntity animatable, ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(this.getTextureLocation(animatable));
    }
}
