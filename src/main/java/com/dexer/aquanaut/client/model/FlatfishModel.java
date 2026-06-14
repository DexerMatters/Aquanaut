package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.FlatfishEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FlatfishModel extends GeoModel<FlatfishEntity> {
    @Override public ResourceLocation getModelResource(FlatfishEntity e) { return rl("geo/flatfish.geo.json"); }
    @Override public ResourceLocation getTextureResource(FlatfishEntity e) { return rl("textures/entity/flatfish.png"); }
    @Override public ResourceLocation getAnimationResource(FlatfishEntity e) { return rl("animations/flatfish.animation.json"); }
    @Override public RenderType getRenderType(FlatfishEntity e, ResourceLocation t) { return RenderType.entityTranslucent(t); }
    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath("aquanaut", p); }
}
