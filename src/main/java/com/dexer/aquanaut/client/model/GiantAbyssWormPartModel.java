package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.GiantAbyssWormEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GiantAbyssWormPartModel extends GeoModel<GiantAbyssWormEntity> {

    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "animations/giant_abyss_worm.animation.json");

    private final ResourceLocation model;
    private final ResourceLocation texture;

    public GiantAbyssWormPartModel(ResourceLocation model, ResourceLocation texture) {
        this.model = model;
        this.texture = texture;
    }

    @Override
    public ResourceLocation getModelResource(GiantAbyssWormEntity animatable) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(GiantAbyssWormEntity animatable) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(GiantAbyssWormEntity animatable) {
        return ANIMATION;
    }
}
