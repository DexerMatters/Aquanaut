package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.AncientNautilusEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class AncientNautilusModel extends GeoModel<AncientNautilusEntity> {
    @Override public ResourceLocation getModelResource(AncientNautilusEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/flagellonautilus.geo.json");
    }
    @Override public ResourceLocation getTextureResource(AncientNautilusEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/flagellonautilus.png");
    }
    @Override public ResourceLocation getAnimationResource(AncientNautilusEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/flagellonautilus.animation.json");
    }
}
