package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.SlimyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SlimyModel extends GeoModel<SlimyEntity> {
    @Override
    public ResourceLocation getModelResource(SlimyEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/slimmy.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SlimyEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/slimmy.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SlimyEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/slimmy.animation.json");
    }
}
