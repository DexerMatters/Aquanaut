package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.FlagellonautilusEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class FlagellonautilusModel extends AquanautGeoModel<FlagellonautilusEntity> {
    @Override
    public ResourceLocation getModelResource(FlagellonautilusEntity entity, GeoRenderer<FlagellonautilusEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/flagellonautilus.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FlagellonautilusEntity entity, GeoRenderer<FlagellonautilusEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/flagellonautilus.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FlagellonautilusEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/flagellonautilus.animation.json");
    }
}
