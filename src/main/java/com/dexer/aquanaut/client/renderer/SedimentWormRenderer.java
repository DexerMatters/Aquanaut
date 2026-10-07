package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.SedimentWormModel;
import com.dexer.aquanaut.common.entity.SedimentWormEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class SedimentWormRenderer extends GeoEntityRenderer<SedimentWormEntity> {
    public SedimentWormRenderer(EntityRendererProvider.Context context) {
        super(context, new SedimentWormModel());
    }
}
