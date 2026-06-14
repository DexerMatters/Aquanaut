package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.DonutfishModel;
import com.dexer.aquanaut.common.entity.DonutfishEntity;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class DonutfishRenderer extends BaseFishRenderer<DonutfishEntity> {
    public DonutfishRenderer(EntityRendererProvider.Context c) {
        super(c, new DonutfishModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
    @Override public @Nullable RenderType getRenderType(DonutfishEntity a, ResourceLocation t,
            @Nullable MultiBufferSource b, float p) { return RenderType.entityTranslucentCull(getTextureLocation(a)); }
}
