package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.core.BiomeRegistry;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

public final class MiddleLevelOceanRegion extends Region {
    public MiddleLevelOceanRegion(int weight) {
        super(MiddleLevelOceanPlacement.location(), RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry,
                          Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        addHiddenBiome(mapper,
                BiomeRegistry.MIDDLE_LEVEL_OCEAN,
                MiddleLevelOceanPlacement.holderAnchorParameter(),
                MiddleLevelOceanPlacement.holderAnchorOffset());
        addHiddenBiome(mapper,
                BiomeRegistry.CORAL_FOREST,
                CoralForestPlacement.holderAnchorParameter(),
                CoralForestPlacement.holderAnchorOffset());
        addHiddenBiome(mapper,
                BiomeRegistry.JELLY_JUNGLE,
                JellyJunglePlacement.holderAnchorParameter(),
                JellyJunglePlacement.holderAnchorOffset());
    }

    private void addHiddenBiome(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper,
                                ResourceKey<Biome> biome,
                                float anchorParameter,
                                float anchorOffset) {
        // Keep the biome in the overworld biome source without allowing climate placement to select it naturally.
        addBiome(mapper,
                Climate.Parameter.point(anchorParameter),
                Climate.Parameter.point(anchorParameter),
                Climate.Parameter.point(anchorParameter),
                Climate.Parameter.point(anchorParameter),
                Climate.Parameter.point(anchorParameter),
                Climate.Parameter.point(anchorParameter),
                anchorOffset,
                biome);
    }
}
