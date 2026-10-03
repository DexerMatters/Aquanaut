package com.dexer.aquanaut.client.renderer;
import com.dexer.aquanaut.client.model.AmbushFishModel;
import com.dexer.aquanaut.common.entity.AmbushFishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
public final class AmbushFishRenderer extends BaseFishRenderer<AmbushFishEntity> {
    public AmbushFishRenderer(EntityRendererProvider.Context context) { super(context, new AmbushFishModel()); }
}
