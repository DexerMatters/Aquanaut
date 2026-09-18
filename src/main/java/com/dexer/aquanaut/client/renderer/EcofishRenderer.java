package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.EcofishModel;
import com.dexer.aquanaut.common.entity.EcofishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class EcofishRenderer extends BaseFishRenderer<EcofishEntity> {
    public EcofishRenderer(EntityRendererProvider.Context context) {
        super(context, new EcofishModel());
    }

    @Override
    public @Nullable RenderType getRenderType(EcofishEntity animatable, ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
