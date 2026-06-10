package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.RedJellyfishEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RedJellyfishModel extends GeoModel<RedJellyfishEntity> {
    @Override public ResourceLocation getModelResource(RedJellyfishEntity e) { return rl("geo/red_jellyfish.geo.json"); }
    @Override public ResourceLocation getTextureResource(RedJellyfishEntity e) { return rl("textures/entity/red_jellyfish.png"); }
    @Override public ResourceLocation getAnimationResource(RedJellyfishEntity e) { return rl("animations/red_jellyfish.animation.json"); }
    @Override public RenderType getRenderType(RedJellyfishEntity e, ResourceLocation t) { return RenderType.entityTranslucent(t); }
    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("aquanaut", path); }
}
