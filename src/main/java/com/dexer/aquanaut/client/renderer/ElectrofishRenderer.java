package com.dexer.aquanaut.client.renderer;

import javax.annotation.Nullable;

import com.dexer.aquanaut.client.model.ElectrofishModel;
import com.dexer.aquanaut.common.entity.ElectrofishEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class ElectrofishRenderer extends BaseFishRenderer<ElectrofishEntity> {
    private static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "textures/entity/electrofish_glowmask.png");

    public ElectrofishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ElectrofishModel());
    }

    @Override
    public @Nullable RenderType getRenderType(ElectrofishEntity animatable, ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(this.getTextureLocation(animatable));
    }

    @Override
    public void actuallyRender(PoseStack poseStack, ElectrofishEntity animatable, BakedGeoModel model,
            @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
            boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {

        float chargeProgress = animatable.getChargeProgress();

        int bodyLight = packedLight;
        int bodyColour = colour;
        if (chargeProgress > 0.01F) {
            bodyLight = (int) Mth.lerp(chargeProgress, (float) packedLight, 15728640.0F);
            int r = (colour >> 16) & 0xFF;
            int g = (colour >> 8) & 0xFF;
            int b = colour & 0xFF;
            int a = (colour >> 24) & 0xFF;
            int rLerp = (int) Mth.lerp(chargeProgress, (float) r, 255.0F);
            int gLerp = (int) Mth.lerp(chargeProgress, (float) g, 255.0F);
            int bLerp = (int) Mth.lerp(chargeProgress, (float) b, 255.0F);
            bodyColour = (a << 24) | (rLerp << 16) | (gLerp << 8) | bLerp;
        }

        if (isReRender) {
            super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, true, partialTick,
                    bodyLight, packedOverlay, bodyColour);
            return;
        }

        RenderType glowRenderType = RenderType.eyes(GLOW_TEXTURE);
        super.actuallyRender(poseStack, animatable, model, glowRenderType, bufferSource,
                bufferSource.getBuffer(glowRenderType), false, partialTick, 15728640, packedOverlay, colour);

        VertexConsumer bodyBuffer = renderType == null ? buffer : bufferSource.getBuffer(renderType);
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, bodyBuffer, true, partialTick,
                bodyLight, packedOverlay, bodyColour);
    }
}
