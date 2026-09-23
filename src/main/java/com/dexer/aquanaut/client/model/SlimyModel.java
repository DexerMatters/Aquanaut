package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.SlimyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class SlimyModel extends AquanautGeoModel<SlimyEntity> {
    @Override
    public ResourceLocation getModelResource(SlimyEntity entity, GeoRenderer<SlimyEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/slimmy.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SlimyEntity entity, GeoRenderer<SlimyEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/slimmy.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SlimyEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/slimmy.animation.json");
    }
}
