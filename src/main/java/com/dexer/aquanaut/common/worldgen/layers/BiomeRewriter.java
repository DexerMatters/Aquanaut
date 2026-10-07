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
 * Spawn-island columns additionally get land surface biomes that match the generated terrain
 * (island hills over the hill country, island plains over the low plateau, beach on the
 * flanks) so vanilla features grow trees, grass and flowers there. The island biomes are
 * mod-owned copies of their vanilla counterparts with the lava lakes, lava springs and
 * vanilla carvers stripped out, so the island keeps vanilla flora and fauna while its
 * caves stay dry and analytically carved.
 */
public final class BiomeRewriter {
    /** Lowest quart Y the island surface biome claims (block Y 40, the beach shelf). */
    static final int ISLAND_MIN_QUART_Y = 10;
    /**
     * Highest quart Y the island surface biome claims: clustered hill tops reach plateau
     * 70 + boosted hill amplitude 26 + dune 1 = block Y 97, quart 24, plus one band of
     * margin.
     */
    static final int ISLAND_MAX_QUART_Y = 25;
    private static final ResourceLocation ISLAND_SURFACE_BIOME =
            ResourceLocation.fromNamespaceAndPath("aquanaut", "island_plains");
    private static final ResourceLocation ISLAND_HILL_BIOME =
            ResourceLocation.fromNamespaceAndPath("aquanaut", "island_hills");
    private static final ResourceLocation ISLAND_SHORE_BIOME =
            ResourceLocation.fromNamespaceAndPath("aquanaut", "island_beach");
    private static final ResourceLocation ISLAND_STONY_SHORE_BIOME =
            ResourceLocation.fromNamespaceAndPath("aquanaut", "island_stony_shore");

    private BiomeRewriter() {
    }

    /** The land biome an island quart cell takes, or {@code null} for the normal stack. */
    enum IslandSurface {
        PLATEAU, HILLS, SHORE, STONY_SHORE
    }

    /**
     * Pure island-biome decision: the stony-shore mining region renders as
     * {@link IslandSurface#STONY_SHORE} (vanilla stone-and-gravel surface rule), raised
     * hill country as {@link IslandSurface#HILLS}, the low grassland as
     * {@link IslandSurface#PLATEAU}, the beach flanks and shelf as
     * {@link IslandSurface#SHORE}; {@code null} falls through to the normal stack. The hill
     * split reads the same relief field the planner lifts the floor with, so the declared
     * biome always matches the ground the player sees. The band runs from block Y 40 up so
     * frozen-ocean iceberg features can never take root on any island column - their
     * placement biome is always a land biome here.
     */
    static IslandSurface islandSurfaceAt(boolean spawnIsland, long islandSeed,
                                         int worldQuartX, int worldQuartZ, int quartY) {
        if (!spawnIsland || quartY < ISLAND_MIN_QUART_Y || quartY > ISLAND_MAX_QUART_Y) {
            return null;
        }
        int blockX = (worldQuartX << 2) + 2;
        int blockZ = (worldQuartZ << 2) + 2;
        if (com.dexer.aquanaut.common.worldgen.blend.SpawnIslandMask.stoneShoreWeight(
                islandSeed, blockX, blockZ) >= 0.5D) {
            return IslandSurface.STONY_SHORE;
        }
        double mask = com.dexer.aquanaut.common.worldgen.blend.SpawnIslandMask.maskAt(
                islandSeed, blockX, blockZ);
        if (mask >= 1.0D) {
            double relief = com.dexer.aquanaut.common.worldgen.blend.SpawnIslandMask.reliefAt(
                    islandSeed, blockX, blockZ, mask);
            return relief >= com.dexer.aquanaut.common.worldgen.blend.SpawnIslandMask.HILL_BIOME_THRESHOLD
                    ? IslandSurface.HILLS
                    : IslandSurface.PLATEAU;
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
        if (island == IslandSurface.HILLS) {
            return ISLAND_HILL_BIOME;
        }
        if (island == IslandSurface.SHORE) {
            return ISLAND_SHORE_BIOME;
        }
        if (island == IslandSurface.STONY_SHORE) {
            return ISLAND_STONY_SHORE_BIOME;
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
