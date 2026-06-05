package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.GiantOctopusTentacleEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GiantOctopusTentacleModel extends GeoModel<GiantOctopusTentacleEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "geo/giant_octopus_tentacle_anchor.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "textures/entity/giant_octopus_tentacle_section.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "animations/giant_octopus_tentacle.animation.json");

    @Override
    public ResourceLocation getModelResource(GiantOctopusTentacleEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GiantOctopusTentacleEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(GiantOctopusTentacleEntity animatable) {
        return ANIMATION;
    }
}