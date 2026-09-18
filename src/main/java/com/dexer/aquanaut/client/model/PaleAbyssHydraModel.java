package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.PaleAbyssHydraEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PaleAbyssHydraModel extends GeoModel<PaleAbyssHydraEntity> {
    @Override
    public ResourceLocation getModelResource(PaleAbyssHydraEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/pale_abyss_hydra.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PaleAbyssHydraEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "textures/entity/pale_abyss_hydra.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PaleAbyssHydraEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/pale_abyss_hydra.animation.json");
    }
}
