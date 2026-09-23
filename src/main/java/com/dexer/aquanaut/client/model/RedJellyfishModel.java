package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.RedJellyfishEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class RedJellyfishModel extends AquanautGeoModel<RedJellyfishEntity> {
    @Override public ResourceLocation getModelResource(RedJellyfishEntity entity, GeoRenderer<RedJellyfishEntity> renderer) { return rl("geo/red_jellyfish.geo.json"); }
    @Override public ResourceLocation getTextureResource(RedJellyfishEntity entity, GeoRenderer<RedJellyfishEntity> renderer) { return rl("textures/entity/red_jellyfish.png"); }
    @Override public ResourceLocation getAnimationResource(RedJellyfishEntity e) { return rl("animations/red_jellyfish.animation.json"); }
    @Override public RenderType getRenderType(RedJellyfishEntity e, ResourceLocation t) { return RenderType.entityTranslucent(t); }
    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("aquanaut", path); }
}
