package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.TripodModel;
import com.dexer.aquanaut.common.entity.TripodEntity;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class TripodRenderer extends BaseFishRenderer<TripodEntity> {
    public TripodRenderer(EntityRendererProvider.Context ctx) { super(ctx, new TripodModel()); }
    @Override public @Nullable RenderType getRenderType(TripodEntity a, ResourceLocation t, @Nullable MultiBufferSource b, float p) {
        return RenderType.entityTranslucent(getTextureLocation(a));
    }
}
