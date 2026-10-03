package com.dexer.aquanaut.client.renderer;
import com.dexer.aquanaut.client.model.GardenEelModel;
import com.dexer.aquanaut.common.entity.GardenEelEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
public final class GardenEelRenderer extends BaseFishRenderer<GardenEelEntity> {
    public GardenEelRenderer(EntityRendererProvider.Context context) { super(context, new GardenEelModel()); }
}
