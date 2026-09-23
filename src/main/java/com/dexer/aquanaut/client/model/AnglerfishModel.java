package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.AnglerfishEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class AnglerfishModel extends AquanautGeoModel<AnglerfishEntity> {
    @Override
    public ResourceLocation getModelResource(AnglerfishEntity entity, GeoRenderer<AnglerfishEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/anglerfish.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AnglerfishEntity entity, GeoRenderer<AnglerfishEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/anglerfish.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AnglerfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/anglerfish.animation.json");
    }
}
