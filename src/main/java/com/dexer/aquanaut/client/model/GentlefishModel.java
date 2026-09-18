package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.GentlefishEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GentlefishModel extends GeoModel<GentlefishEntity> {
    @Override
    public ResourceLocation getModelResource(GentlefishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/gentlefish.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GentlefishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/gentlefish.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GentlefishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/gentlefish.animation.json");
    }
}
