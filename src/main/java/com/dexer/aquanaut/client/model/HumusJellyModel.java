package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.HumusJellyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class HumusJellyModel extends GeoModel<HumusJellyEntity> {
    @Override public ResourceLocation getModelResource(HumusJellyEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/blue_jellyfish.geo.json");
    }
    @Override public ResourceLocation getTextureResource(HumusJellyEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/blue_jellyfish.png");
    }
    @Override public ResourceLocation getAnimationResource(HumusJellyEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/blue_jellyfish.animation.json");
    }
}
