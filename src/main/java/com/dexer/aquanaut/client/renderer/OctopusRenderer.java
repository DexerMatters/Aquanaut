package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.OctopusModel;
import com.dexer.aquanaut.common.entity.OctopusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class OctopusRenderer extends BaseFishRenderer<OctopusEntity> {
    public OctopusRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new OctopusModel());
    }
}
