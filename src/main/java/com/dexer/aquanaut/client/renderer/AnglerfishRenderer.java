package com.dexer.aquanaut.client.renderer;

import org.jetbrains.annotations.Nullable;

import com.dexer.aquanaut.client.model.AnglerfishModel;
import com.dexer.aquanaut.common.entity.AnglerfishEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class AnglerfishRenderer extends BaseFishRenderer<AnglerfishEntity> {
    public AnglerfishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AnglerfishModel());
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public @Nullable RenderType getRenderType(AnglerfishEntity animatable, ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(this.getTextureLocation(animatable));
    }
}
