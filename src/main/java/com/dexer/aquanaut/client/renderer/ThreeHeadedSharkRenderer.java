package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.ThreeHeadedSharkModel;
import com.dexer.aquanaut.common.entity.ThreeHeadedSharkEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ThreeHeadedSharkRenderer extends BaseFishRenderer<ThreeHeadedSharkEntity> {
    public ThreeHeadedSharkRenderer(EntityRendererProvider.Context context) {
        super(context, new ThreeHeadedSharkModel());
    }
}
