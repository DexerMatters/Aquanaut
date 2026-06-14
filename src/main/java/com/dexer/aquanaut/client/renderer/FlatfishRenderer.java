package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.FlatfishModel;
import com.dexer.aquanaut.common.entity.FlatfishEntity;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class FlatfishRenderer extends BaseFishRenderer<FlatfishEntity> {
    public FlatfishRenderer(EntityRendererProvider.Context c) { super(c, new FlatfishModel()); }
    @Override public @Nullable RenderType getRenderType(FlatfishEntity a, ResourceLocation t,
            @Nullable MultiBufferSource b, float p) { return RenderType.entityTranslucent(getTextureLocation(a)); }
}
