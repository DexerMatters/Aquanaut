package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.GentlefishModel;
import com.dexer.aquanaut.common.entity.GentlefishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class GentlefishRenderer extends BaseFishRenderer<GentlefishEntity> {
    public GentlefishRenderer(EntityRendererProvider.Context context) {
        super(context, new GentlefishModel());
    }
}
