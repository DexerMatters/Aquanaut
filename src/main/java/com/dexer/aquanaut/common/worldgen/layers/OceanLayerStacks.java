package com.dexer.aquanaut.common.worldgen.layers;

import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime registry of ocean layer stacks. Datapacks may replace or add stacks at reload;
 * the default stack is always available as a fallback.
 */
public final class OceanLayerStacks {
    public static final ResourceLocation DEFAULT_ID =
            ResourceLocation.fromNamespaceAndPath("aquanaut", "default_deep_stack");

    private static final Map<ResourceLocation, OceanLayerStack> STACKS = new ConcurrentHashMap<>();
    private static volatile ResourceLocation activeId = DEFAULT_ID;

    static {
        STACKS.put(DEFAULT_ID, loadBundledOrDefault());
    }

    private OceanLayerStacks() {
    }

    private static OceanLayerStack loadBundledOrDefault() {
        String path = "/data/aquanaut/worldgen/ocean_layer_stack/default_deep_stack.json";
        try (InputStream stream = OceanLayerStacks.class.getResourceAsStream(path)) {
            if (stream != null) {
                return OceanLayerStackJson.fromStream(stream);
            }
        } catch (Exception ignored) {
            // Fall through to the Java default so unit tests and early class-load stay safe.
        }
        return DefaultOceanLayerStacks.defaultDeepStack();
    }

    public static void register(OceanLayerStack stack) {
        STACKS.put(stack.id(), stack);
    }

    public static void replaceAll(Collection<OceanLayerStack> stacks) {
        STACKS.clear();
        STACKS.put(DEFAULT_ID, DefaultOceanLayerStacks.defaultDeepStack());
        for (OceanLayerStack stack : stacks) {
            STACKS.put(stack.id(), stack);
        }
        if (!STACKS.containsKey(activeId)) {
            activeId = DEFAULT_ID;
        }
    }

    public static void setActive(ResourceLocation id) {
        if (STACKS.containsKey(id)) {
            activeId = id;
        }
    }

    public static OceanLayerStack active() {
        OceanLayerStack stack = STACKS.get(activeId);
        return stack != null ? stack : STACKS.get(DEFAULT_ID);
    }

    public static OceanLayerStack get(ResourceLocation id) {
        return STACKS.get(id);
    }

    public static OceanLayerStack defaultStack() {
        return STACKS.get(DEFAULT_ID);
    }

    public static TerrainModule terrain(ResourceLocation id) {
        return DefaultOceanLayerStacks.terrain(id);
    }
}
