package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.SlimyModel;
import com.dexer.aquanaut.common.entity.SlimyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class SlimyRenderer extends BaseFishRenderer<SlimyEntity> {
    public SlimyRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimyModel());
    }
}
