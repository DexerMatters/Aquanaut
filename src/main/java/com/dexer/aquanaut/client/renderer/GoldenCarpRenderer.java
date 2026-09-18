package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.GoldenCarpModel;
import com.dexer.aquanaut.common.entity.GoldenCarpEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class GoldenCarpRenderer extends BaseFishRenderer<GoldenCarpEntity> {
    public GoldenCarpRenderer(EntityRendererProvider.Context context) {
        super(context, new GoldenCarpModel());
    }
}
