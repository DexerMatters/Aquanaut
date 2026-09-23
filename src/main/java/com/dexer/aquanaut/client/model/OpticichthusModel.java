package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.OpticichthusEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class OpticichthusModel extends AquanautGeoModel<OpticichthusEntity> {
    @Override
    public ResourceLocation getModelResource(OpticichthusEntity entity, GeoRenderer<OpticichthusEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/opticichthus.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(OpticichthusEntity entity, GeoRenderer<OpticichthusEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/opticichthus.png");
    }

    @Override
    public ResourceLocation getAnimationResource(OpticichthusEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/opticichthus.animation.json");
    }
}
