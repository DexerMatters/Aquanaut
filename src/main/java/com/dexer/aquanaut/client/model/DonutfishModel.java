package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.DonutfishEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DonutfishModel extends GeoModel<DonutfishEntity> {
    @Override
    public ResourceLocation getModelResource(DonutfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/donutfish.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DonutfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/donutfish.png");
    }

    @Override
    public RenderType getRenderType(DonutfishEntity animatable, ResourceLocation texture) {
        return RenderType.entityTranslucentCull(texture);
    }

    @Override
    public ResourceLocation getAnimationResource(DonutfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/donutfish.animation.json");
    }
}
