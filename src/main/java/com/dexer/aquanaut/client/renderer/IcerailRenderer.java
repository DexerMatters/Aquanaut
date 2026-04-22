package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.IcerailModel;
import com.dexer.aquanaut.common.entity.IcerailEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class IcerailRenderer extends BaseFishRenderer<IcerailEntity> {
    public IcerailRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new IcerailModel());
    }
}
