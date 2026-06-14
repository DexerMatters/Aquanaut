package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.worldgen.CoralForestPillarFeature;
import com.dexer.aquanaut.common.worldgen.JellyJungleBulgeFeature;
import com.dexer.aquanaut.common.worldgen.JellyJungleCrackFeature;
import com.dexer.aquanaut.common.worldgen.JellyJungleStemForestFeature;
import com.dexer.aquanaut.common.worldgen.JellyJungleVegetationFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FeatureRegistry {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE,
            Aquanaut.MODID);
    public static final DeferredHolder<Feature<?>, CoralForestPillarFeature> CORAL_FOREST_PILLAR = FEATURES.register(
            "coral_forest_pillar",
            CoralForestPillarFeature::new);
    public static final DeferredHolder<Feature<?>, JellyJungleBulgeFeature> JELLY_JUNGLE_BULGE = FEATURES.register(
            "jelly_jungle_bulge",
            JellyJungleBulgeFeature::new);
    public static final DeferredHolder<Feature<?>, JellyJungleCrackFeature> JELLY_JUNGLE_CRACK = FEATURES.register(
            "jelly_jungle_crack",
            JellyJungleCrackFeature::new);
    public static final DeferredHolder<Feature<?>, JellyJungleVegetationFeature> JELLY_JUNGLE_VEGETATION =
            FEATURES.register("jelly_jungle_vegetation",
                    JellyJungleVegetationFeature::new);
    public static final DeferredHolder<Feature<?>, JellyJungleStemForestFeature> JELLY_JUNGLE_STEM_FOREST =
            FEATURES.register("jelly_jungle_stem_forest",
                    JellyJungleStemForestFeature::new);

    private FeatureRegistry() {
    }

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
