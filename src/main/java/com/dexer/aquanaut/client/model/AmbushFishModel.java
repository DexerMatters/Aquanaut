package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.AmbushFishEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class AmbushFishModel extends GeoModel<AmbushFishEntity> {
    public ResourceLocation getModelResource(AmbushFishEntity e) { return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/ambush_fish.geo.json"); }
    public ResourceLocation getTextureResource(AmbushFishEntity e) { return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/ambush_fish.png"); }
    public ResourceLocation getAnimationResource(AmbushFishEntity e) { return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/ambush_fish.animation.json"); }
}
