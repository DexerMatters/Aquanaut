package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.AnglerfishEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AnglerfishModel extends GeoModel<AnglerfishEntity> {
    @Override
    public ResourceLocation getModelResource(AnglerfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/anglerfish.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AnglerfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/anglerfish.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AnglerfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/anglerfish.animation.json");
    }
}
