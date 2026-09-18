package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.EcofishEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EcofishModel extends GeoModel<EcofishEntity> {
    @Override
    public ResourceLocation getModelResource(EcofishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/ecofish.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EcofishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/ecofish.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EcofishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/ecofish.animation.json");
    }
}
