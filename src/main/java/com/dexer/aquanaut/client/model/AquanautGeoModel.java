package com.dexer.aquanaut.client.model;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * {@link GeoModel} with the deprecated resource accessors satisfied in exactly one place.
 *
 * <p>
 * GeckoLib still declares {@code getModelResource(T)} and {@code getTextureResource(T)} abstract,
 * yet marks both deprecated in favour of the renderer-aware overloads. A concrete model therefore
 * has to implement a deprecated method no matter what it does. This class implements those two
 * once, forwarding to the overloads, so every model in this package overrides only the
 * non-deprecated form and none carries a suppression of its own.
 *
 * @param <T> the animatable the model describes
 */
public abstract class AquanautGeoModel<T extends GeoAnimatable> extends GeoModel<T> {

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(T animatable) {
        return getModelResource(animatable, null);
    }

    public abstract ResourceLocation getModelResource(T entity, GeoRenderer<T> renderer);

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(T animatable) {
        return getTextureResource(animatable, null);
    }

    public abstract ResourceLocation getTextureResource(T entity, GeoRenderer<T> renderer);
}
