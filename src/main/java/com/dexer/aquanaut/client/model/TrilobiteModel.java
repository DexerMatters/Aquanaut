package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.TrilobiteEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class TrilobiteModel extends GeoModel<TrilobiteEntity> {
    @Override public ResourceLocation getModelResource(TrilobiteEntity e) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/trilobite.geo.json");
    }
    @Override public ResourceLocation getTextureResource(TrilobiteEntity e) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/trilobite.png");
    }
    @Override public ResourceLocation getAnimationResource(TrilobiteEntity e) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/trilobite.animation.json");
    }
}
