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
 * Spawn-island columns additionally get a land surface biome (sunflower plains over the
 * plateau, beach on the flanks) so vanilla features grow trees, grass and flowers there.
 */
public final class BiomeRewriter {
    /** Lowest quart Y the island surface biome claims (block Y 60, below the Y70 plateau). */
    static final int ISLAND_MIN_QUART_Y = 15;
    /** Highest quart Y the island surface biome claims (block Y 71, the plateau top). */
    static final int ISLAND_MAX_QUART_Y = 17;
    private static final ResourceLocation ISLAND_SURFACE_BIOME =
            ResourceLocation.fromNamespaceAndPath("minecraft", "sunflower_plains");
    private static final ResourceLocation ISLAND_SHORE_BIOME =
            ResourceLocation.fromNamespaceAndPath("minecraft", "beach");

    private BiomeRewriter() {
    }

    /** The land biome an island quart cell takes, or {@code null} for the normal stack. */
    enum IslandSurface {
        PLATEAU, SHORE
    }

    /**
     * Pure island-biome decision: {@link IslandSurface#PLATEAU} on the fully emerged flat
     * core (trees and flowers grow there), {@link IslandSurface#SHORE} on the beach flanks,
     * {@code null} outside the island's surface band or when the island is inactive.
     */
    static IslandSurface islandSurfaceAt(boolean spawnIsland, long islandSeed,
                                         int worldQuartX, int worldQuartZ, int quartY) {
        if (!spawnIsland || quartY < ISLAND_MIN_QUART_Y || quartY > ISLAND_MAX_QUART_Y) {
            return null;
        }
        double mask = com.dexer.aquanaut.common.worldgen.blend.SpawnIslandMask.maskAt(islandSeed,
                (worldQuartX << 2) + 2, (worldQuartZ << 2) + 2);
        if (mask >= 1.0D) {
            return IslandSurface.PLATEAU;
        }
        if (mask > 0.0D) {
            return IslandSurface.SHORE;
        }
        return null;
    }

    public static void rewrite(net.minecraft.world.level.chunk.ChunkAccess chunk,
                               OceanGenSampler sampler,
                               BiomeSource biomeSource,
                               java.util.function.Function<ResourceLocation, Holder<Biome>> registryLookup) {
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
        if (sampler.spawnIsland()) {
            // The island's land biomes live above every stack rewrite band.
            maxQuartY = Math.max(maxQuartY, Math.min(
                    net.minecraft.core.QuartPos.fromBlock(chunk.getMinBuildHeight() + chunk.getHeight()) - 1,
                    ISLAND_MAX_QUART_Y));
        }
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
                        // Island surface biomes are vanilla-only and absent from the biome
                        // source; fall back to the full biome registry for those.
                        holder = registryLookup.apply(target);
                        holderCache.put(target, holder);
                    }
                    if (holder == null) {
                        continue;
                    }

                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(
                            net.minecraft.core.QuartPos.toBlock(quartY)));
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
        IslandSurface island = islandSurfaceAt(sampler.spawnIsland(), sampler.islandSeed(),
                worldQuartX, worldQuartZ, quartY);
        if (island == IslandSurface.PLATEAU) {
            return ISLAND_SURFACE_BIOME;
        }
        if (island == IslandSurface.SHORE) {
            return ISLAND_SHORE_BIOME;
        }
        if (quartY > stack.maxRewriteQuartY()) {
            return null;
        }
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
