package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.ElectrofishEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ElectrofishModel extends GeoModel<ElectrofishEntity> {
    @Override
    public ResourceLocation getModelResource(ElectrofishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/electrofish.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ElectrofishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/eletrofish.png");
    }

    @Override
    public RenderType getRenderType(ElectrofishEntity animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public ResourceLocation getAnimationResource(ElectrofishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/electrofish.animation.json");
    }
}
