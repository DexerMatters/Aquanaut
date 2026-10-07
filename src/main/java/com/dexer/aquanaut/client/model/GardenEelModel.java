package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.GardenEelEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class GardenEelModel extends GeoModel<GardenEelEntity> {
    public ResourceLocation getModelResource(GardenEelEntity e) { return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/garden_eel.geo.json"); }
    public ResourceLocation getTextureResource(GardenEelEntity e) { return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/garden_eel.png"); }
    public ResourceLocation getAnimationResource(GardenEelEntity e) { return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/garden_eel.animation.json"); }
}
