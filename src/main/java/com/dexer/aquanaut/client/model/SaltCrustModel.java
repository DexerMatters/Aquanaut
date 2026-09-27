package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.SaltCrustEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class SaltCrustModel extends AquanautGeoModel<SaltCrustEntity> {
    @Override
    public ResourceLocation getModelResource(SaltCrustEntity entity, GeoRenderer<SaltCrustEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/salt_crust.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SaltCrustEntity entity, GeoRenderer<SaltCrustEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/salt_crust.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SaltCrustEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/salt_crust.animation.json");
    }
}
