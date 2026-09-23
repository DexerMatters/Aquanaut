package com.dexer.aquanaut.core;

import com.dexer.aquanaut.common.worldgen.BrimstoneCalderaPlacement;
import com.dexer.aquanaut.common.worldgen.BrineMirrorGorgePlacement;
import com.dexer.aquanaut.common.worldgen.CoralForestPlacement;
import com.dexer.aquanaut.common.worldgen.JellyJunglePlacement;
import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanPlacement;
import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanRegion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import terrablender.api.Regions;

public final class BiomeRegistry {
    public static final ResourceKey<Biome> MIDDLE_LEVEL_OCEAN = ResourceKey.create(Registries.BIOME,
            MiddleLevelOceanPlacement.location());
    public static final ResourceKey<Biome> CORAL_FOREST = ResourceKey.create(Registries.BIOME,
            CoralForestPlacement.location());
    public static final ResourceKey<Biome> JELLY_JUNGLE = ResourceKey.create(Registries.BIOME,
            JellyJunglePlacement.location());
    public static final ResourceKey<Biome> BRINE_MIRROR_GORGE = ResourceKey.create(Registries.BIOME,
            BrineMirrorGorgePlacement.location());
    public static final ResourceKey<Biome> BRIMSTONE_CALDERA = ResourceKey.create(Registries.BIOME,
            BrimstoneCalderaPlacement.location());

    private BiomeRegistry() {
    }

    public static void register() {
        Regions.register(new MiddleLevelOceanRegion(MiddleLevelOceanPlacement.regionWeight()));
    }
}
