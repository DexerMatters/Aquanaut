package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.entity.BiologicalDetectorEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * The detector's geometry, drawn from the shipped GeckoLib assets.
 *
 * <p>
 * One skin, and one mask to go with it: GeckoLib's glow layer derives
 * {@code _glowmask} from
 * whatever base texture this returns, so {@code biological_detector.png} is lit
 * by
 * {@code biological_detector_glowmask.png} — the bio-green collar, the array
 * cells on the belt, the
 * ring seams, the pole tell-tales and the sensor core inside the shell.
 */
public class BiologicalDetectorModel extends AquanautGeoModel<BiologicalDetectorEntity> {

    private static final ResourceLocation TEXTURE = rl("textures/entity/biological_detector.png");

    @Override
    public ResourceLocation getModelResource(BiologicalDetectorEntity entity,
            GeoRenderer<BiologicalDetectorEntity> renderer) {
        return rl("geo/biological_detector.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BiologicalDetectorEntity entity,
            GeoRenderer<BiologicalDetectorEntity> renderer) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BiologicalDetectorEntity entity) {
        return rl("animations/biological_detector.animation.json");
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", path);
    }
}
