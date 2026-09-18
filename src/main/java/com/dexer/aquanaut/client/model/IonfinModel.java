package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.IonfinEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class IonfinModel extends GeoModel<IonfinEntity> {
    @Override
    public ResourceLocation getModelResource(IonfinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/ionfin.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(IonfinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/ionfin.png");
    }

    @Override
    public ResourceLocation getAnimationResource(IonfinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/ionfin.animation.json");
    }
}
