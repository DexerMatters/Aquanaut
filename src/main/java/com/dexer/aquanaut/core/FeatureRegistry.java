package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.worldgen.CoralForestPillarFeature;
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

    private FeatureRegistry() {
    }

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
