package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.SaltCrustModel;
import com.dexer.aquanaut.common.entity.SaltCrustEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders the salt crust's latching turn. The model is authored level; while the
 * creature eases {@link SaltCrustEntity#getAttachPitch} onto a pillar the renderer
 * pitches the whole body up about its core and slides it toward the surface, so the
 * ventral foot lies flat against the vertical salt instead of simply facing it.
 */
public class SaltCrustRenderer extends GeoEntityRenderer<SaltCrustEntity> {
    /** Degrees of pitch that counts as fully latched. */
    private static final float LATCHED_TILT = 88.0F;
    /** Model-space height of the shell core the body swings around, in model units. */
    private static final double PIVOT_MODEL_Y = 7.0D;
    /** How far the belly slides onto the salt at full tilt, in blocks. */
    private static final double LATCH_PUSH = 0.15D;

    public SaltCrustRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SaltCrustModel());
    }

    @Override
    protected void applyRotations(SaltCrustEntity animatable, PoseStack poseStack, float ageInTicks,
            float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);

        float pitch = animatable.getAttachPitch(partialTick);
        if (Math.abs(pitch) <= 0.05F) {
            return;
        }

        double amount = Math.min(1.0D, Math.abs(pitch) / LATCHED_TILT);
        // Slide the belly onto the salt so the creature reads as glued to the pillar.
        poseStack.translate(0.0D, 0.0D, -LATCH_PUSH * amount);
        // Swing about the shell core: the body lies against the surface instead of rearing over it.
        double pivotY = (PIVOT_MODEL_Y / 16.0D) / nativeScale;
        poseStack.translate(0.0D, pivotY, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.translate(0.0D, -pivotY, 0.0D);
    }
}
