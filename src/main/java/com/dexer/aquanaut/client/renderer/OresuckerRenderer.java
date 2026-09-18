package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.OresuckerModel;
import com.dexer.aquanaut.common.entity.OresuckerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class OresuckerRenderer extends BaseFishRenderer<OresuckerEntity> {
    public OresuckerRenderer(EntityRendererProvider.Context context) {
        super(context, new OresuckerModel());
    }
}
