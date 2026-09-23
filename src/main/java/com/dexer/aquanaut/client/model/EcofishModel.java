package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.EcofishEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class EcofishModel extends AquanautGeoModel<EcofishEntity> {
    @Override
    public ResourceLocation getModelResource(EcofishEntity entity, GeoRenderer<EcofishEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/ecofish.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EcofishEntity entity, GeoRenderer<EcofishEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/ecofish.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EcofishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/ecofish.animation.json");
    }
}
