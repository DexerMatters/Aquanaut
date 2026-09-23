package com.dexer.aquanaut.common.worldgen.layers;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * Writes the stereoscopic biome stack into section palettes for supported quart columns.
 * Only configured rewrite Y bands (plus soft margins) are touched — not the full world height.
 */
public final class BiomeRewriter {
    private BiomeRewriter() {
    }

    public static void rewrite(net.minecraft.world.level.chunk.ChunkAccess chunk,
                               OceanGenSampler sampler,
                               BiomeSource biomeSource) {
        OceanLayerStack stack = sampler.stack();
        if (!sampler.anySupported()) {
            return;
        }

        Map<ResourceLocation, Holder<Biome>> holderCache = new HashMap<>();
        int minQuartY = Math.max(
                net.minecraft.core.QuartPos.fromBlock(chunk.getMinBuildHeight()),
                stack.minRewriteQuartY());
        int maxQuartY = Math.min(
                net.minecraft.core.QuartPos.fromBlock(chunk.getMinBuildHeight() + chunk.getHeight()) - 1,
                stack.maxRewriteQuartY());
        if (maxQuartY < minQuartY) {
            return;
        }

        int baseQuartX = sampler.baseQuartX();
        int baseQuartZ = sampler.baseQuartZ();

        for (int localQuartX = 0; localQuartX < OceanGenSampler.QUARTS; localQuartX++) {
            for (int localQuartZ = 0; localQuartZ < OceanGenSampler.QUARTS; localQuartZ++) {
                if (!sampler.supportsCurrentChunkCell(localQuartX, localQuartZ)) {
                    continue;
                }
                int worldQuartX = baseQuartX + localQuartX;
                int worldQuartZ = baseQuartZ + localQuartZ;

                for (int quartY = minQuartY; quartY <= maxQuartY; quartY++) {
                    ResourceLocation target = dominantBiome(stack, sampler, worldQuartX, worldQuartZ,
                            quartY, localQuartX, localQuartZ);
                    if (target == null) {
                        continue;
                    }
                    Holder<Biome> holder = holderCache.computeIfAbsent(target,
                            id -> findBiome(biomeSource, id));
                    if (holder == null) {
                        holderCache.put(target, null);
                        continue;
                    }

                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(
                            net.minecraft.core.QuartPos.toBlock(quartY)));
                    @SuppressWarnings("unchecked")
                    PalettedContainer<Holder<Biome>> biomes =
                            (PalettedContainer<Holder<Biome>>) section.getBiomes();
                    biomes.set(localQuartX, Math.floorMod(quartY, 4), localQuartZ, holder);
                }
            }
        }
    }

    private static ResourceLocation dominantBiome(OceanLayerStack stack,
                                                  OceanGenSampler sampler,
                                                  int worldQuartX,
                                                  int worldQuartZ,
                                                  int quartY,
                                                  int localQuartX,
                                                  int localQuartZ) {
        int blockY = QuartY.toBlockCenter(quartY);
        double best = 0.0D;
        ResourceLocation bestBiome = null;
        double edge = sampler.edgeStrengthAtLocalBlock(localQuartX * 4 + 1, localQuartZ * 4 + 1);

        for (OceanLayer layer : stack.layers()) {
            if (!layer.rewritesBiome()) {
                continue;
            }
            double vertical = layer.band().weightAtBlockY(blockY);
            if (vertical <= 0.0D) {
                continue;
            }
            // Region edge gates custom layers so borders melt into vanilla terrain.
            double gated = vertical * edge;
            if (gated <= 0.0D) {
                continue;
            }
            double[] mix = layer.mix().weightsAt(worldQuartX, worldQuartZ);
            for (int i = 0; i < mix.length; i++) {
                MixEntry entry = layer.mix().entries().get(i);
                if (entry.inheritSurface()) {
                    continue;
                }
                double w = gated * mix[i];
                if (w > best) {
                    best = w;
                    bestBiome = entry.biome();
                }
            }
        }
        return bestBiome;
    }

    private static Holder<Biome> findBiome(BiomeSource biomeSource, ResourceLocation biomeLocation) {
        for (Holder<Biome> biome : biomeSource.possibleBiomes()) {
            if (biome.is(biomeLocation)) {
                return biome;
            }
        }
        return null;
    }
}
