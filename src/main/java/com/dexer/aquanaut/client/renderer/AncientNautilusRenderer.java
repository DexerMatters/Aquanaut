package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.AncientNautilusModel;
import com.dexer.aquanaut.common.entity.AncientNautilusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class AncientNautilusRenderer extends BaseFishRenderer<AncientNautilusEntity> {
    public AncientNautilusRenderer(EntityRendererProvider.Context context) {
        super(context, new AncientNautilusModel());
    }
}
