package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.VampreyModel;
import com.dexer.aquanaut.common.entity.VampreyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class VampreyRenderer extends BaseFishRenderer<VampreyEntity> {
    public VampreyRenderer(EntityRendererProvider.Context context) {
        super(context, new VampreyModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
