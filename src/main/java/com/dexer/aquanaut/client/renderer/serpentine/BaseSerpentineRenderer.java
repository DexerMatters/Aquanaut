package com.dexer.aquanaut.client.renderer.serpentine;

import com.dexer.aquanaut.common.entity.serpentine.AbstractSerpentineEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public abstract class BaseSerpentineRenderer<T extends AbstractSerpentineEntity & GeoEntity>
        extends GeoEntityRenderer<T> {

    protected BaseSerpentineRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    @Override
    protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks,
            float rotationYaw, float partialTick, float nativeScale) {
        float entityYaw = Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));

        double modelOffsetY = hitboxVisualYOffset(animatable);
        if (Math.abs(modelOffsetY) > 1.0E-4D) {
            poseStack.translate(0.0D, modelOffsetY / nativeScale, 0.0D);
        }

        if (!applyWholeModelPitch(animatable))
            return;

        float pitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());
        if (Math.abs(pitch) < 0.01F)
            return;

        double desiredWorldPivotY = animatable.getBbHeight() * 0.5D + hitboxPitchPivotOffsetY(animatable);
        double pivotY = (desiredWorldPivotY - modelOffsetY) / nativeScale;
        poseStack.translate(0.0D, pivotY, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.translate(0.0D, -pivotY, 0.0D);
    }

    protected double hitboxVisualYOffset(T entity) {
        return 0.0D;
    }

    protected boolean applyWholeModelPitch(T entity) {
        return false;
    }

    protected double hitboxPitchPivotOffsetY(T entity) {
        return 0.0D;
    }
}
