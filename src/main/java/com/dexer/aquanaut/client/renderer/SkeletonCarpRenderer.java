package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.SkeletonCarpModel;
import com.dexer.aquanaut.common.entity.SkeletonCarpEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class SkeletonCarpRenderer extends BaseFishRenderer<SkeletonCarpEntity> {
    public SkeletonCarpRenderer(EntityRendererProvider.Context context) {
        super(context, new SkeletonCarpModel());
    }
}
