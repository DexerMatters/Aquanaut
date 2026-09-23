package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.SpringfishEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class SpringfishModel extends AquanautGeoModel<SpringfishEntity> {
    @Override
    public ResourceLocation getModelResource(SpringfishEntity entity, GeoRenderer<SpringfishEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/springfish.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SpringfishEntity entity, GeoRenderer<SpringfishEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/springfish.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SpringfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/springfish.animation.json");
    }
}
