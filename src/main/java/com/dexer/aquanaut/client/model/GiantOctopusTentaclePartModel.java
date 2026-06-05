package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.GiantOctopusTentacleEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GiantOctopusTentaclePartModel extends GeoModel<GiantOctopusTentacleEntity> {

    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("aquanaut",
            "animations/giant_octopus_tentacle.animation.json");

    private final ResourceLocation model;
    private final ResourceLocation texture;

    public GiantOctopusTentaclePartModel(ResourceLocation model, ResourceLocation texture) {
        this.model = model;
        this.texture = texture;
    }

    @Override
    public ResourceLocation getModelResource(GiantOctopusTentacleEntity animatable) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(GiantOctopusTentacleEntity animatable) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(GiantOctopusTentacleEntity animatable) {
        return ANIMATION;
    }
}