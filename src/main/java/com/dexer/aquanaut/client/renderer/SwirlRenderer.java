package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.common.entity.SwirlEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Minimal renderer for SwirlEntity — all visuals are handled by particles.
 */
public class SwirlRenderer extends EntityRenderer<SwirlEntity> {

    private static final ResourceLocation EMPTY = ResourceLocation.withDefaultNamespace("");

    public SwirlRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SwirlEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        // No visual rendering; all visuals are particle-based
    }

    @Override
    public ResourceLocation getTextureLocation(SwirlEntity entity) {
        return EMPTY;
    }
}
