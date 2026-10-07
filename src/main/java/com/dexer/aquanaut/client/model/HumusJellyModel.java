package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.HumusJellyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class HumusJellyModel extends GeoModel<HumusJellyEntity> {
    @Override public ResourceLocation getModelResource(HumusJellyEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/humus_jelly.geo.json");
    }
    @Override public ResourceLocation getTextureResource(HumusJellyEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/humus_jelly.png");
    }
    @Override public ResourceLocation getAnimationResource(HumusJellyEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/humus_jelly.animation.json");
    }
}
