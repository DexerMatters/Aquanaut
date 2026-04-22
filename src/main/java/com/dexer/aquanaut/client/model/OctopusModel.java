package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.OctopusEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class OctopusModel extends GeoModel<OctopusEntity> {
    @Override
    public ResourceLocation getModelResource(OctopusEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/octopus.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(OctopusEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/octopus.png");
    }

    @Override
    public ResourceLocation getAnimationResource(OctopusEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/octopus.animation.json");
    }
}
