package com.dexer.aquanaut.client.renderer;

import javax.annotation.Nullable;

import com.dexer.aquanaut.client.model.SwirlMakerModel;
import com.dexer.aquanaut.common.entity.SwirlMakerEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class SwirlMakerRenderer extends BaseFishRenderer<SwirlMakerEntity> {

    public SwirlMakerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SwirlMakerModel());
    }

    @Override
    public @Nullable RenderType getRenderType(SwirlMakerEntity animatable, ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(this.getTextureLocation(animatable));
    }
}
