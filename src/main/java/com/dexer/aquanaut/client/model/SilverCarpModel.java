package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.SilverCarpEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class SilverCarpModel extends AquanautGeoModel<SilverCarpEntity> {
    @Override
    public ResourceLocation getModelResource(SilverCarpEntity entity, GeoRenderer<SilverCarpEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/silver_carp.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SilverCarpEntity entity, GeoRenderer<SilverCarpEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/silver_carp.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SilverCarpEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/silver_carp.animation.json");
    }
}
