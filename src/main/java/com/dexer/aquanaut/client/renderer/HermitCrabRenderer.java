package com.dexer.aquanaut.client.renderer;

import javax.annotation.Nullable;

import com.dexer.aquanaut.client.model.HermitCrabModel;
import com.dexer.aquanaut.common.entity.HermitCrabEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class HermitCrabRenderer extends GeoEntityRenderer<HermitCrabEntity> {
    /** Extra model size per shell tier, so a grown crab reads visibly bigger and taller. */
    private static final float SCALE_PER_TIER = 0.16F;

    public HermitCrabRenderer(EntityRendererProvider.Context context) { super(context, new HermitCrabModel()); }

    @Override
    public void actuallyRender(PoseStack poseStack, HermitCrabEntity animatable, BakedGeoModel model,
            @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
            boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        float scale = 1.0F + animatable.shellSize() * SCALE_PER_TIER;
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);
        poseStack.popPose();
    }
}
