package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.OctopusEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class OctopusModel extends AquanautGeoModel<OctopusEntity> {
    @Override
    public ResourceLocation getModelResource(OctopusEntity entity, GeoRenderer<OctopusEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/octopus.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(OctopusEntity entity, GeoRenderer<OctopusEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/octopus.png");
    }

    @Override
    public ResourceLocation getAnimationResource(OctopusEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/octopus.animation.json");
    }
}
