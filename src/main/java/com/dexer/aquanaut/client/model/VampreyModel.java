package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.VampreyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class VampreyModel extends GeoModel<VampreyEntity> {
    @Override
    public ResourceLocation getModelResource(VampreyEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/vamprey.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(VampreyEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/vamprey.png");
    }

    @Override
    public ResourceLocation getAnimationResource(VampreyEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/vamprey.animation.json");
    }
}
