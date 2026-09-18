package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.SkeletonCarpEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SkeletonCarpModel extends GeoModel<SkeletonCarpEntity> {
    @Override
    public ResourceLocation getModelResource(SkeletonCarpEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/skeleton_carp.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SkeletonCarpEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/skeleton_carp.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SkeletonCarpEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/skeleton_carp.animation.json");
    }
}
