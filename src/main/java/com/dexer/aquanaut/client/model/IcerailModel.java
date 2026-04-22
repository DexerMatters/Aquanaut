package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.IcerailEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class IcerailModel extends GeoModel<IcerailEntity> {
    @Override
    public ResourceLocation getModelResource(IcerailEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/icerail.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(IcerailEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/icerail.png");
    }

    @Override
    public RenderType getRenderType(IcerailEntity animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public ResourceLocation getAnimationResource(IcerailEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/icerail.animation.json");
    }
}
