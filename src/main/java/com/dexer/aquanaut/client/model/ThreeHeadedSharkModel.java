package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.ThreeHeadedSharkEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ThreeHeadedSharkModel extends GeoModel<ThreeHeadedSharkEntity> {
    @Override
    public ResourceLocation getModelResource(ThreeHeadedSharkEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/three_headed_shark.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ThreeHeadedSharkEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/three_headed_shark.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ThreeHeadedSharkEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/three_headed_shark.animation.json");
    }
}
