package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.MantaRayEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class MantaRayModel extends AquanautGeoModel<MantaRayEntity> {
    @Override
    public ResourceLocation getModelResource(MantaRayEntity entity, GeoRenderer<MantaRayEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/manta_ray.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MantaRayEntity entity, GeoRenderer<MantaRayEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/manta_ray.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MantaRayEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/manta_ray.animation.json");
    }
}
