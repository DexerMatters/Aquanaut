package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.CreeporpedoEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CreeporpedoModel extends GeoModel<CreeporpedoEntity> {
    @Override
    public ResourceLocation getModelResource(CreeporpedoEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/creeporpedo.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CreeporpedoEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/creeporpedo.png");
    }

    @Override
    public RenderType getRenderType(CreeporpedoEntity animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public ResourceLocation getAnimationResource(CreeporpedoEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/creeporpedo.animation.json");
    }
}
