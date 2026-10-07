package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.MudSilverfishEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class MudSilverfishModel extends GeoModel<MudSilverfishEntity> {
    @Override public ResourceLocation getModelResource(MudSilverfishEntity e) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/mud_silverfish.geo.json");
    }
    @Override public ResourceLocation getTextureResource(MudSilverfishEntity e) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/mud_silverfish.png");
    }
    @Override public ResourceLocation getAnimationResource(MudSilverfishEntity e) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/mud_silverfish.animation.json");
    }
}
