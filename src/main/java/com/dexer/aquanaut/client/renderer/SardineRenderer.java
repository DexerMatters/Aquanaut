package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.SardineModel;
import com.dexer.aquanaut.common.entity.SardineEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class SardineRenderer extends BaseFishRenderer<SardineEntity> {
    public SardineRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SardineModel());
    }
}
