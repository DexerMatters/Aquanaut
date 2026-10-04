package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.TrilobiteModel;
import com.dexer.aquanaut.common.entity.TrilobiteEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class TrilobiteRenderer extends GeoEntityRenderer<TrilobiteEntity> {
    public TrilobiteRenderer(EntityRendererProvider.Context context) {
        super(context, new TrilobiteModel());
    }
}
