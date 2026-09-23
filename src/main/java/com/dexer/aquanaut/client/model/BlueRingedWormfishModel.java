package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.BlueRingedWormfishEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class BlueRingedWormfishModel extends AquanautGeoModel<BlueRingedWormfishEntity> {
    @Override public ResourceLocation getModelResource(BlueRingedWormfishEntity entity, GeoRenderer<BlueRingedWormfishEntity> renderer) { return rl("geo/blue_ringed_wormfish.geo.json"); }
    @Override public ResourceLocation getTextureResource(BlueRingedWormfishEntity entity, GeoRenderer<BlueRingedWormfishEntity> renderer) { return rl("textures/entity/blue_ringed_wormfish.png"); }
    @Override public ResourceLocation getAnimationResource(BlueRingedWormfishEntity e) { return rl("animations/blue_ringed_wormfish.animation.json"); }
    @Override public RenderType getRenderType(BlueRingedWormfishEntity e, ResourceLocation t) { return RenderType.entityTranslucent(t); }
    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath("aquanaut", p); }
}
