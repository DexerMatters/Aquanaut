package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.SubmarineDroneEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * The drone's geometry, with two skins.
 *
 * <p>
 * A powered drone wears the bright stainless hull with a lit lens, shroud band and floodlight; a
 * powered-down one wears the same hull gone cold, with the lens dark and the lamps out. The choice
 * is driven by the drone's synced control state, so the hull visibly dies the moment the link is
 * dropped.
 *
 * <p>
 * GeckoLib derives the glow layer's mask from whatever this returns by appending {@code _glowmask},
 * so both skins need a matching mask of the same size:
 * {@code submarine_drone_glowmask.png} and {@code submarine_drone_deactivated_glowmask.png}.
 */
public class SubmarineDroneModel extends AquanautGeoModel<SubmarineDroneEntity> {

    private static final ResourceLocation ACTIVE_TEXTURE = rl("textures/entity/submarine_drone.png");
    private static final ResourceLocation INACTIVE_TEXTURE = rl("textures/entity/submarine_drone_deactivated.png");

    @Override
    public ResourceLocation getModelResource(SubmarineDroneEntity entity, GeoRenderer<SubmarineDroneEntity> renderer) {
        return rl("geo/submarine_drone.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SubmarineDroneEntity entity, GeoRenderer<SubmarineDroneEntity> renderer) {
        return entity.isControlled() ? ACTIVE_TEXTURE : INACTIVE_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SubmarineDroneEntity entity) {
        return rl("animations/submarine_drone.animation.json");
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", path);
    }
}
