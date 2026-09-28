package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.block.entity.InvestigationBoardBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

/** Resource model for the static investigation board display. */
public class InvestigationBoardGeoModel extends AquanautGeoModel<InvestigationBoardBlockEntity> {
    private static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", path);
    }

    @Override
    public ResourceLocation getModelResource(InvestigationBoardBlockEntity entity,
            GeoRenderer<InvestigationBoardBlockEntity> renderer) {
        return resource("geo/investigation_board.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(InvestigationBoardBlockEntity entity,
            GeoRenderer<InvestigationBoardBlockEntity> renderer) {
        return resource("textures/block/investigation_board.png");
    }

    @Override
    public ResourceLocation getAnimationResource(InvestigationBoardBlockEntity entity) {
        return resource("animations/investigation_board.animation.json");
    }
}
