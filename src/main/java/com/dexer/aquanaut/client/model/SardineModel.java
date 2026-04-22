package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.SardineEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SardineModel extends GeoModel<SardineEntity> {
    @Override
    public ResourceLocation getModelResource(SardineEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/sardine.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SardineEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/sardine.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SardineEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/sardine.animation.json");
    }
}
