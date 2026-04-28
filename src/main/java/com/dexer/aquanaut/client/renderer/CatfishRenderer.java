package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.CatfishModel;
import com.dexer.aquanaut.common.entity.CatfishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class CatfishRenderer extends BaseFishRenderer<CatfishEntity> {
    public CatfishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CatfishModel());
    }
}
