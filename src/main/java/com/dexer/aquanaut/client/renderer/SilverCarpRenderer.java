package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.SilverCarpModel;
import com.dexer.aquanaut.common.entity.SilverCarpEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class SilverCarpRenderer extends BaseFishRenderer<SilverCarpEntity> {
    public SilverCarpRenderer(EntityRendererProvider.Context context) {
        super(context, new SilverCarpModel());
    }
}
