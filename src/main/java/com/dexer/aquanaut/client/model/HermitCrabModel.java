package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.HermitCrabEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class HermitCrabModel extends GeoModel<HermitCrabEntity> {
    /** A grown crab reads its larger shell geometry; the base shell otherwise. */
    public ResourceLocation getModelResource(HermitCrabEntity e) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut",
                e.shellSize() > 0 ? "geo/hermit_crab_large.geo.json" : "geo/hermit_crab.geo.json");
    }
    public ResourceLocation getTextureResource(HermitCrabEntity e) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut",
                e.shellSize() > 0 ? "textures/entity/hermit_crab_large_shell.png"
                        : "textures/entity/hermit_crab.png");
    }
    public ResourceLocation getAnimationResource(HermitCrabEntity e) { return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/hermit_crab.animation.json"); }
}
