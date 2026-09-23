package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.LightingWormEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class LightingWormModel extends AquanautGeoModel<LightingWormEntity> {
    @Override
    public ResourceLocation getModelResource(LightingWormEntity entity, GeoRenderer<LightingWormEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/lighting_worm.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LightingWormEntity entity, GeoRenderer<LightingWormEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/lighting_worm.png");
    }

    @Override
    public RenderType getRenderType(LightingWormEntity animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public ResourceLocation getAnimationResource(LightingWormEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/lighting_worm.animation.json");
    }
}
