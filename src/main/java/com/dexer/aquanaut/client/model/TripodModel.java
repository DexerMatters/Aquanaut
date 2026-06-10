package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.TripodEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TripodModel extends GeoModel<TripodEntity> {
    @Override public ResourceLocation getModelResource(TripodEntity e) { return rl("geo/tripod.geo.json"); }
    @Override public ResourceLocation getTextureResource(TripodEntity e) { return rl("textures/entity/tripod.png"); }
    @Override public ResourceLocation getAnimationResource(TripodEntity e) { return rl("animations/tripod.animation.json"); }
    @Override public RenderType getRenderType(TripodEntity e, ResourceLocation t) { return RenderType.entityTranslucent(t); }
    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("aquanaut", path); }
}
