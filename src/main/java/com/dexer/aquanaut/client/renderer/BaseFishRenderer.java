package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.common.entity.BaseFishEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BaseFishRenderer<T extends BaseFishEntity & GeoEntity> extends GeoEntityRenderer<T> {
    public BaseFishRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
    }

    @Override
    protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);

        // Jet-propelled swimmers travel shell-first while they are moving, which is the half turn.
        // The attack animation turns the shell around onto the victim itself, so its own half turn is
        // dropped for the duration of the attack: applying both would cancel out and strike backwards.
        boolean tailFirst = animatable.swimsTailFirst() && !animatable.isFacingThreat();
        if (tailFirst) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        }

        double modelOffsetY = animatable.hitboxVisualYOffset();
        if (Math.abs(modelOffsetY) > 1.0E-4D) {
            poseStack.translate(0.0D, modelOffsetY / nativeScale, 0.0D);
        }

        float pitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());
        if (tailFirst) {
            // The shell leads the dive, so the pitch is mirrored along with the heading.
            pitch = -pitch;
        }
        if (Math.abs(pitch) < 0.01F || !animatable.shouldApplyPitchRotation()) {
            return;
        }

        double desiredWorldPivotY = animatable.getBbHeight() * 0.5D + animatable.hitboxPitchPivotOffsetY();
        double pivotY = (desiredWorldPivotY - modelOffsetY) / nativeScale;
        poseStack.translate(0.0D, pivotY, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.translate(0.0D, -pivotY, 0.0D);
    }
}
