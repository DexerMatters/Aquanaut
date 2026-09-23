package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.SwirlMakerEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class SwirlMakerModel extends AquanautGeoModel<SwirlMakerEntity> {
    @Override
    public ResourceLocation getModelResource(SwirlMakerEntity entity, GeoRenderer<SwirlMakerEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/swirl_maker.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SwirlMakerEntity entity, GeoRenderer<SwirlMakerEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/swirl_maker.png");
    }

    @Override
    public RenderType getRenderType(SwirlMakerEntity animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public ResourceLocation getAnimationResource(SwirlMakerEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/swirl_maker.animation.json");
    }
}
