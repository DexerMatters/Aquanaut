package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.SedimentWormEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class SedimentWormModel extends GeoModel<SedimentWormEntity> {
    @Override public ResourceLocation getModelResource(SedimentWormEntity e) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/sediment_worm.geo.json");
    }
    @Override public ResourceLocation getTextureResource(SedimentWormEntity e) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/sediment_worm.png");
    }
    @Override public ResourceLocation getAnimationResource(SedimentWormEntity e) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/sediment_worm.animation.json");
    }
}
