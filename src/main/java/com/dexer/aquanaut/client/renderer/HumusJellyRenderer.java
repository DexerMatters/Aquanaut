package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.HumusJellyModel;
import com.dexer.aquanaut.common.entity.HumusJellyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class HumusJellyRenderer extends BaseFishRenderer<HumusJellyEntity> {
    public HumusJellyRenderer(EntityRendererProvider.Context context) {
        super(context, new HumusJellyModel());
    }
}
