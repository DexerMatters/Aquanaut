package com.dexer.aquanaut.client.renderer;

import com.dexer.aquanaut.client.model.MudSilverfishModel;
import com.dexer.aquanaut.common.entity.MudSilverfishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class MudSilverfishRenderer extends GeoEntityRenderer<MudSilverfishEntity> {
    public MudSilverfishRenderer(EntityRendererProvider.Context context) {
        super(context, new MudSilverfishModel());
    }
}
