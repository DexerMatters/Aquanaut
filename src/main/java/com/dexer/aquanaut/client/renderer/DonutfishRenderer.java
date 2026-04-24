package com.dexer.aquanaut.client.renderer;

import javax.annotation.Nullable;

import com.dexer.aquanaut.client.model.DonutfishModel;
import com.dexer.aquanaut.common.entity.DonutfishEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class DonutfishRenderer extends BaseFishRenderer<DonutfishEntity> {
    private static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "textures/entity/donutfish_glowmask.png");

    public DonutfishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DonutfishModel());
    }

    @Override
    public @Nullable RenderType getRenderType(DonutfishEntity animatable, ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucentCull(this.getTextureLocation(animatable));
    }

    @Override
    public void actuallyRender(PoseStack poseStack, DonutfishEntity animatable, BakedGeoModel model,
            @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
            boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (isReRender) {
            super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, true, partialTick,
                    packedLight, packedOverlay, colour);
            return;
        }

        RenderType bodyRenderType = renderType == null
                ? RenderType.entityTranslucentCull(this.getTextureLocation(animatable))
                : renderType;
        VertexConsumer bodyBuffer = bufferSource.getBuffer(bodyRenderType);
        super.actuallyRender(poseStack, animatable, model, bodyRenderType, bufferSource, bodyBuffer, true,
                partialTick, packedLight, packedOverlay, colour);

        RenderType glowRenderType = RenderType.eyes(GLOW_TEXTURE);
        super.actuallyRender(poseStack, animatable, model, glowRenderType, bufferSource,
                bufferSource.getBuffer(glowRenderType), false, partialTick, 15728640, packedOverlay, colour);
    }
}
