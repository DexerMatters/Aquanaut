package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.OxygenBreederEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class OxygenBreederModel extends GeoModel<OxygenBreederEntity> {
    @Override public ResourceLocation getModelResource(OxygenBreederEntity e) { return rl("geo/oxygen_breeder.geo.json"); }
    @Override public ResourceLocation getTextureResource(OxygenBreederEntity e) { return rl("textures/entity/oxygen_breeder.png"); }
    @Override public ResourceLocation getAnimationResource(OxygenBreederEntity e) { return rl("animations/oxygen_breeder.animation.json"); }
    @Override public RenderType getRenderType(OxygenBreederEntity e, ResourceLocation t) { return RenderType.entityTranslucent(t); }
    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("aquanaut", path); }
}
