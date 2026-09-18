package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.PaleAbyssHydraModel;
import com.dexer.aquanaut.common.entity.PaleAbyssHydraEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class PaleAbyssHydraRenderer extends BaseFishRenderer<PaleAbyssHydraEntity> {
    public PaleAbyssHydraRenderer(EntityRendererProvider.Context context) {
        super(context, new PaleAbyssHydraModel());
    }
}
