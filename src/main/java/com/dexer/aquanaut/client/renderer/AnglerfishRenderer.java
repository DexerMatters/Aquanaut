package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.AnglerfishModel;
import com.dexer.aquanaut.common.entity.AnglerfishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class AnglerfishRenderer extends BaseFishRenderer<AnglerfishEntity> {
    public AnglerfishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AnglerfishModel());
    }
}
