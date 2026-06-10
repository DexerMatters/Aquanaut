package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.RadioanemoneEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RadioanemoneModel extends GeoModel<RadioanemoneEntity> {
    @Override public ResourceLocation getModelResource(RadioanemoneEntity e) { return rl("geo/radioanemone.geo.json"); }
    @Override public ResourceLocation getTextureResource(RadioanemoneEntity e) { return rl("textures/entity/radioanemone.png"); }
    @Override public ResourceLocation getAnimationResource(RadioanemoneEntity e) { return rl("animations/radioanemone.animation.json"); }
    @Override public RenderType getRenderType(RadioanemoneEntity e, ResourceLocation t) { return RenderType.entityTranslucent(t); }
    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("aquanaut", path); }
}
