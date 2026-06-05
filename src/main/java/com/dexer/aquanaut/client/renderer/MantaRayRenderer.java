package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.MantaRayModel;
import com.dexer.aquanaut.common.entity.MantaRayEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class MantaRayRenderer extends BaseFishRenderer<MantaRayEntity> {

    public MantaRayRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MantaRayModel());
    }

    @Override
    protected void applyRotations(MantaRayEntity animatable, PoseStack poseStack, float ageInTicks,
            float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }
}
