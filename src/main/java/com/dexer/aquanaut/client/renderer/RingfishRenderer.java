package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.RingfishModel;
import com.dexer.aquanaut.common.entity.RingfishEntity;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class RingfishRenderer extends BaseFishRenderer<RingfishEntity> {
    public RingfishRenderer(EntityRendererProvider.Context ctx) { super(ctx, new RingfishModel()); }
    @Override public @Nullable RenderType getRenderType(RingfishEntity a, ResourceLocation t, @Nullable MultiBufferSource b, float p) {
        return RenderType.entityTranslucent(getTextureLocation(a));
    }
}
