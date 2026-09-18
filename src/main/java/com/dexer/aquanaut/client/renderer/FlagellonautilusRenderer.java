package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.FlagellonautilusModel;
import com.dexer.aquanaut.common.entity.FlagellonautilusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class FlagellonautilusRenderer extends BaseFishRenderer<FlagellonautilusEntity> {
    public FlagellonautilusRenderer(EntityRendererProvider.Context context) {
        super(context, new FlagellonautilusModel());
    }
}
