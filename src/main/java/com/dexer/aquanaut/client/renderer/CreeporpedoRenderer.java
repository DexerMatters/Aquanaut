package com.dexer.aquanaut.client.renderer;

import javax.annotation.Nullable;

import com.dexer.aquanaut.client.model.CreeporpedoModel;
import com.dexer.aquanaut.common.entity.CreeporpedoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.util.Color;

public class CreeporpedoRenderer extends BaseFishRenderer<CreeporpedoEntity> {

    public CreeporpedoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CreeporpedoModel());
    }

    @Override
    public @Nullable RenderType getRenderType(CreeporpedoEntity animatable, ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(this.getTextureLocation(animatable));
    }

    @Override
    public Color getRenderColor(CreeporpedoEntity animatable, float partialTick, int packedLight) {
        float progress = animatable.getIgniteProgress();
        if (progress <= 0.0F) return Color.ofRGB(1.0F, 1.0F, 1.0F);
        float boost = 1.0F + progress * 1.2F;
        return Color.ofRGB(boost, boost, boost);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, CreeporpedoEntity animatable, BakedGeoModel model,
            @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
            boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        float progress = animatable.getIgniteProgress();

        if (progress <= 0.0F) {
            super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                    isReRender, partialTick, packedLight, packedOverlay, colour);
            return;
        }

        float scale = 1.0F + progress * 0.2F;
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);

        poseStack.popPose();
    }
}
