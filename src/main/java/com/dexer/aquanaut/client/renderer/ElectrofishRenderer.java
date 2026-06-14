package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.ElectrofishModel;
import com.dexer.aquanaut.common.entity.ElectrofishEntity;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class ElectrofishRenderer extends BaseFishRenderer<ElectrofishEntity> {
    public ElectrofishRenderer(EntityRendererProvider.Context c) {
        super(c, new ElectrofishModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
    @Override public @Nullable RenderType getRenderType(ElectrofishEntity a, ResourceLocation t,
            @Nullable MultiBufferSource b, float p) { return RenderType.entityTranslucent(getTextureLocation(a)); }
}
