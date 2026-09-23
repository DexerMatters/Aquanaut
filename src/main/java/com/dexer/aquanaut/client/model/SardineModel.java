package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.SardineEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class SardineModel extends AquanautGeoModel<SardineEntity> {
    @Override
    public ResourceLocation getModelResource(SardineEntity entity, GeoRenderer<SardineEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/sardine.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SardineEntity entity, GeoRenderer<SardineEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/sardine.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SardineEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/sardine.animation.json");
    }
}
