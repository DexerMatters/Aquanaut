package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.CursorEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class CursorModel extends AquanautGeoModel<CursorEntity> {
    @Override
    public ResourceLocation getModelResource(CursorEntity entity, GeoRenderer<CursorEntity> renderer) {
        return rl("geo/cursor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CursorEntity entity, GeoRenderer<CursorEntity> renderer) {
        return rl("textures/entity/cursor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CursorEntity entity) {
        return rl("animations/cursor.animation.json");
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", path);
    }
}
