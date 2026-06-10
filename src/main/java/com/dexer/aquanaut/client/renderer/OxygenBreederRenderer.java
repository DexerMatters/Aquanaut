package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.OxygenBreederModel;
import com.dexer.aquanaut.common.entity.OxygenBreederEntity;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class OxygenBreederRenderer extends BaseFishRenderer<OxygenBreederEntity> {
    public OxygenBreederRenderer(EntityRendererProvider.Context ctx) { super(ctx, new OxygenBreederModel()); }
    @Override public @Nullable RenderType getRenderType(OxygenBreederEntity a, ResourceLocation t, @Nullable MultiBufferSource b, float p) {
        return RenderType.entityTranslucent(getTextureLocation(a));
    }
}
