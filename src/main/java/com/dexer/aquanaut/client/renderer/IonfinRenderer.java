package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.IonfinModel;
import com.dexer.aquanaut.common.entity.IonfinEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class IonfinRenderer extends BaseFishRenderer<IonfinEntity> {
    public IonfinRenderer(EntityRendererProvider.Context context) {
        super(context, new IonfinModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
