package com.dexer.aquanaut.client.renderer;
import com.dexer.aquanaut.client.model.HermitCrabModel;
import com.dexer.aquanaut.common.entity.HermitCrabEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public final class HermitCrabRenderer extends GeoEntityRenderer<HermitCrabEntity> {
    public HermitCrabRenderer(EntityRendererProvider.Context context) { super(context, new HermitCrabModel()); }
}
