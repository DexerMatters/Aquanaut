package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.CatfishEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class CatfishModel extends AquanautGeoModel<CatfishEntity> {
    @Override
    public ResourceLocation getModelResource(CatfishEntity entity, GeoRenderer<CatfishEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/catfish.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CatfishEntity entity, GeoRenderer<CatfishEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/catfish.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CatfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/catfish.animation.json");
    }
}
