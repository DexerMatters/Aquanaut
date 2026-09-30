package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

/**
 * Keys for the world presets this mod contributes through data packs.
 *
 * <p>
 * The water world itself is pure data: {@code data/aquanaut/worldgen/world_preset/water_world.json}
 * pairs an all-deep-ocean biome source with {@code aquanaut:water_world} noise settings whose
 * continents function is pinned to a constant deep-ocean value, so every column is open water and
 * the mod's ocean layer stack qualifies everywhere. The {@code minecraft:normal} world preset tag
 * adds it to the world type cycle on the create-world screen, and the translation key
 * {@code generator.aquanaut.water_world} names it.
 */
public final class WorldPresetRegistry {
    public static final ResourceKey<WorldPreset> WATER_WORLD = ResourceKey.create(Registries.WORLD_PRESET,
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "water_world"));

    private WorldPresetRegistry() {
    }
}
