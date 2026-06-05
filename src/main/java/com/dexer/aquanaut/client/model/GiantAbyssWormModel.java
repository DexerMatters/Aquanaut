package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.GiantAbyssWormEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GiantAbyssWormModel extends GeoModel<GiantAbyssWormEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "geo/giant_abyss_worm_head.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "textures/entity/giant_abyss_worm_head.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "animations/giant_abyss_worm.animation.json");

    @Override
    public ResourceLocation getModelResource(GiantAbyssWormEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GiantAbyssWormEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(GiantAbyssWormEntity animatable) {
        return ANIMATION;
    }
}
