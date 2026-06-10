package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.GloomgazerModel;
import com.dexer.aquanaut.common.entity.GloomgazerEntity;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class GloomgazerRenderer extends BaseFishRenderer<GloomgazerEntity> {
    public GloomgazerRenderer(EntityRendererProvider.Context ctx) { super(ctx, new GloomgazerModel()); }
    @Override public @Nullable RenderType getRenderType(GloomgazerEntity a, ResourceLocation t, @Nullable MultiBufferSource b, float p) {
        return RenderType.entityTranslucent(getTextureLocation(a));
    }
}
