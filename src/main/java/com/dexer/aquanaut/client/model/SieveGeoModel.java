package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.block.entity.SieveBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * Resource model for the static sifting sieve.
 *
 * <p>
 * The geometry is a single-block model authored centred on the block's footprint with its floor at
 * {@code y = 0}, which is what GeckoLib's block renderer expects; no animation file is shipped
 * because the sieve has no animated part.
 */
public class SieveGeoModel extends AquanautGeoModel<SieveBlockEntity> {
    private static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", path);
    }

    @Override
    public ResourceLocation getModelResource(SieveBlockEntity entity, GeoRenderer<SieveBlockEntity> renderer) {
        return resource("geo/sieve.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SieveBlockEntity entity, GeoRenderer<SieveBlockEntity> renderer) {
        return resource("textures/block/sieve.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SieveBlockEntity entity) {
        // Static sieve: an empty animation file keeps GeckoLib's resource contract satisfied.
        return resource("animations/sieve.animation.json");
    }
}
