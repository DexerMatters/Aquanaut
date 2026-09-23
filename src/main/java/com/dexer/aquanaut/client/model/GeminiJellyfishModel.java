package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.GeminiJellyfishEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class GeminiJellyfishModel extends AquanautGeoModel<GeminiJellyfishEntity> {
    @Override
    public ResourceLocation getModelResource(GeminiJellyfishEntity entity, GeoRenderer<GeminiJellyfishEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/gemini_jellyfish.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeminiJellyfishEntity entity, GeoRenderer<GeminiJellyfishEntity> renderer) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/gemini_jellyfish.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GeminiJellyfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/gemini_jellyfish.animation.json");
    }
}
