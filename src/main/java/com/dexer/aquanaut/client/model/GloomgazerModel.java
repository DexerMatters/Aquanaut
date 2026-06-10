package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.GloomgazerEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GloomgazerModel extends GeoModel<GloomgazerEntity> {
    @Override public ResourceLocation getModelResource(GloomgazerEntity e) { return rl("geo/gloomgazer.geo.json"); }
    @Override public ResourceLocation getTextureResource(GloomgazerEntity e) { return rl("textures/entity/gloomgazer.png"); }
    @Override public ResourceLocation getAnimationResource(GloomgazerEntity e) { return rl("animations/gloomgazer.animation.json"); }
    @Override public RenderType getRenderType(GloomgazerEntity e, ResourceLocation t) { return RenderType.entityTranslucent(t); }
    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("aquanaut", path); }
}
