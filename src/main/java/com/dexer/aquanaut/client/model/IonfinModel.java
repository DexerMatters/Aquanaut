package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.IonfinEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class IonfinModel extends AquanautGeoModel<IonfinEntity> {
    @Override
    public ResourceLocation getModelResource(IonfinEntity entity, GeoRenderer<IonfinEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/ionfin.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(IonfinEntity entity, GeoRenderer<IonfinEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/ionfin.png");
    }

    @Override
    public ResourceLocation getAnimationResource(IonfinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/ionfin.animation.json");
    }
}
