package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.RedJellyfishModel;
import com.dexer.aquanaut.common.entity.RedJellyfishEntity;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class RedJellyfishRenderer extends BaseFishRenderer<RedJellyfishEntity> {
    public RedJellyfishRenderer(EntityRendererProvider.Context c) {
        super(c, new RedJellyfishModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
    @Override public @Nullable RenderType getRenderType(RedJellyfishEntity a, ResourceLocation t,
            @Nullable MultiBufferSource b, float p) { return RenderType.entityTranslucent(getTextureLocation(a)); }
}
