package com.dexer.aquanaut.common.worldgen.layers;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in default stack + terrain modules. Mirrors the legacy 3-band layout with soft edges.
 */
public final class DefaultOceanLayerStacks {
    private static final Map<ResourceLocation, TerrainModule> TERRAIN = new ConcurrentHashMap<>();

    static {
        TERRAIN.put(ResourceLocation.fromNamespaceAndPath("aquanaut", "reef_cap"), TerrainModule.reefCap());
        TERRAIN.put(ResourceLocation.fromNamespaceAndPath("aquanaut", "middle_cavity"), TerrainModule.middleCavity());
        TERRAIN.put(ResourceLocation.withDefaultNamespace("none"), TerrainModule.none());
    }

    private DefaultOceanLayerStacks() {
    }

    public static OceanLayerStack defaultDeepStack() {
        return new OceanLayerStack(
                OceanLayerStacks.DEFAULT_ID,
                List.of(OceanLayer.surface(), OceanLayer.reefCeiling(), OceanLayer.middleSea()),
                1.0D,
                16.0D,
                16,
                List.of(
                        ResourceLocation.withDefaultNamespace("deep_ocean"),
                        ResourceLocation.withDefaultNamespace("deep_cold_ocean"),
                        ResourceLocation.withDefaultNamespace("deep_lukewarm_ocean"),
                        ResourceLocation.withDefaultNamespace("deep_frozen_ocean")
                ));
    }

    public static void registerTerrain(ResourceLocation id, TerrainModule module) {
        TERRAIN.put(id, module);
    }

    public static TerrainModule terrain(ResourceLocation id) {
        TerrainModule module = TERRAIN.get(id);
        return module != null ? module : TERRAIN.get(ResourceLocation.withDefaultNamespace("none"));
    }
}
