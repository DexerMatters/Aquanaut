package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.GoldenCarpEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GoldenCarpModel extends GeoModel<GoldenCarpEntity> {
    @Override
    public ResourceLocation getModelResource(GoldenCarpEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/golden_carp.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GoldenCarpEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/golden_carp.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GoldenCarpEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/golden_carp.animation.json");
    }
}
