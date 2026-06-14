package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.BlueJellyfishEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BlueJellyfishModel extends GeoModel<BlueJellyfishEntity> {
    @Override public ResourceLocation getModelResource(BlueJellyfishEntity e) { return rl("geo/blue_jellyfish.geo.json"); }
    @Override public ResourceLocation getTextureResource(BlueJellyfishEntity e) { return rl("textures/entity/blue_jellyfish.png"); }
    @Override public ResourceLocation getAnimationResource(BlueJellyfishEntity e) { return rl("animations/blue_jellyfish.animation.json"); }
    @Override public RenderType getRenderType(BlueJellyfishEntity e, ResourceLocation t) { return RenderType.entityTranslucent(t); }
    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath("aquanaut", p); }
}
