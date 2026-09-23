package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.OresuckerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class OresuckerModel extends AquanautGeoModel<OresuckerEntity> {
    @Override
    public ResourceLocation getModelResource(OresuckerEntity entity, GeoRenderer<OresuckerEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/oresucker.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(OresuckerEntity entity, GeoRenderer<OresuckerEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/oresucker.png");
    }

    @Override
    public ResourceLocation getAnimationResource(OresuckerEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/oresucker.animation.json");
    }
}
