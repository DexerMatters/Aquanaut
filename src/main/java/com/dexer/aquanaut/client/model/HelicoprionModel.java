package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.HelicoprionEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HelicoprionModel extends GeoModel<HelicoprionEntity> {
    @Override
    public ResourceLocation getModelResource(HelicoprionEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/helicoprion.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HelicoprionEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/helicoprion.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HelicoprionEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/helicoprion.animation.json");
    }
}
