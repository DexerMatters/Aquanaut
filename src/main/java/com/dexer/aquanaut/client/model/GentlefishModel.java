package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.GentlefishEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class GentlefishModel extends AquanautGeoModel<GentlefishEntity> {
    @Override
    public ResourceLocation getModelResource(GentlefishEntity entity, GeoRenderer<GentlefishEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/gentlefish.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GentlefishEntity entity, GeoRenderer<GentlefishEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/gentlefish.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GentlefishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/gentlefish.animation.json");
    }
}
