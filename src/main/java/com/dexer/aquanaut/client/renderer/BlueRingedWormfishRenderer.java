package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.BlueRingedWormfishModel;
import com.dexer.aquanaut.common.entity.BlueRingedWormfishEntity;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class BlueRingedWormfishRenderer extends BaseFishRenderer<BlueRingedWormfishEntity> {
    public BlueRingedWormfishRenderer(EntityRendererProvider.Context c) {
        super(c, new BlueRingedWormfishModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
    @Override public @Nullable RenderType getRenderType(BlueRingedWormfishEntity a, ResourceLocation t,
            @Nullable MultiBufferSource b, float p) { return RenderType.entityTranslucent(getTextureLocation(a)); }
}
