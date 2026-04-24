package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.SpringfishModel;
import com.dexer.aquanaut.common.entity.SpringfishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class SpringfishRenderer extends BaseFishRenderer<SpringfishEntity> {
    public SpringfishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SpringfishModel());
    }
}
