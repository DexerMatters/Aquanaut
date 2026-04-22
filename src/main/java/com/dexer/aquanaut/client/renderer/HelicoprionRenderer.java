package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.HelicoprionModel;
import com.dexer.aquanaut.common.entity.HelicoprionEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class HelicoprionRenderer extends BaseFishRenderer<HelicoprionEntity> {
    public HelicoprionRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new HelicoprionModel());
    }
}
