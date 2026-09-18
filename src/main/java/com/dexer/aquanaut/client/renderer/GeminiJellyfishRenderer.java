package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.GeminiJellyfishModel;
import com.dexer.aquanaut.common.entity.GeminiJellyfishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class GeminiJellyfishRenderer extends BaseFishRenderer<GeminiJellyfishEntity> {
    public GeminiJellyfishRenderer(EntityRendererProvider.Context context) {
        super(context, new GeminiJellyfishModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public @Nullable RenderType getRenderType(GeminiJellyfishEntity animatable, ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
