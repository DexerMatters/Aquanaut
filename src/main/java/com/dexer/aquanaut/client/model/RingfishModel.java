package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.RingfishEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RingfishModel extends GeoModel<RingfishEntity> {
    @Override public ResourceLocation getModelResource(RingfishEntity e) { return rl("geo/ringfish.geo.json"); }
    @Override public ResourceLocation getTextureResource(RingfishEntity e) { return rl("textures/entity/ringfish.png"); }
    @Override public ResourceLocation getAnimationResource(RingfishEntity e) { return rl("animations/ringfish.animation.json"); }
    @Override public RenderType getRenderType(RingfishEntity e, ResourceLocation t) { return RenderType.entityTranslucent(t); }
    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("aquanaut", path); }
}
