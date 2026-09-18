package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.FlagellonautilusEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FlagellonautilusModel extends GeoModel<FlagellonautilusEntity> {
    @Override
    public ResourceLocation getModelResource(FlagellonautilusEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/flagellonautilus.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FlagellonautilusEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/flagellonautilus.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FlagellonautilusEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/flagellonautilus.animation.json");
    }
}
