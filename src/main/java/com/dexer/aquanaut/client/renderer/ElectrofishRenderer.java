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
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class ElectrofishRenderer extends BaseFishRenderer<ElectrofishEntity> {
    private static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "textures/entity/eletrofish_glowmask.png");

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
        if (isReRender) {
            super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, true, partialTick,
                    packedLight, packedOverlay, colour);
            return;
        }

        RenderType glowRenderType = RenderType.eyes(GLOW_TEXTURE);
        super.actuallyRender(poseStack, animatable, model, glowRenderType, bufferSource,
                bufferSource.getBuffer(glowRenderType), false, partialTick, 15728640, packedOverlay, colour);

        VertexConsumer bodyBuffer = renderType == null ? buffer : bufferSource.getBuffer(renderType);
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, bodyBuffer, true, partialTick,
                packedLight, packedOverlay, colour);
    }
}
