package com.dexer.aquanaut.common.worldgen.layers;

import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanPlacement;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;

/**
 * Coarse per-chunk sampling: 16 quart-center open-water probes (not a full second 16×16 noise pass).
 */
public final class OceanChunkSampler {
    private OceanChunkSampler() {
    }

    /**
     * @param probeFactory creates a short-lived NoiseChunk used only for Y=62 open-water probes
     */
    public static OceanGenSampler sample(ChunkAccess chunk,
                                         OceanLayerStack stack,
                                         int topWaterY,
                                         boolean spawnIsland,
                                         int minCellY,
                                         int cellCountY,
                                         int cellWidth,
                                         int cellHeight,
                                         BlockState defaultBlock,
                                         java.util.function.Supplier<NoiseChunk> probeFactory,
                                         java.util.function.BiFunction<Integer, Integer, ResourceLocation> haloBiomeAt) {
        ChunkPos chunkPos = chunk.getPos();
        int baseQuartX = QuartPos.fromBlock(chunkPos.getMinBlockX());
        int baseQuartZ = QuartPos.fromBlock(chunkPos.getMinBlockZ());
        boolean[] quartOpenWater = new boolean[16];
        ResourceLocation[][] surfaceBiomes = new ResourceLocation[4][4];
        int surfaceQuartY = MiddleLevelOceanPlacement.surfaceSampleQuartY();

        for (int lx = 0; lx < 4; lx++) {
            for (int lz = 0; lz < 4; lz++) {
                surfaceBiomes[lx][lz] = chunk.getNoiseBiome(baseQuartX + lx, surfaceQuartY, baseQuartZ + lz)
                        .unwrapKey()
                        .map(key -> key.location())
                        .orElse(null);
            }
        }

        int sampleCellY = Math.floorDiv(topWaterY, cellHeight) - minCellY;
        int sampleOffsetY = Math.floorMod(topWaterY, cellHeight);
        if (sampleCellY >= 0 && sampleCellY < cellCountY) {
            NoiseChunk probe = probeFactory.get();
            probe.initializeForFirstCellX();
            for (int cx = 0; cx < 16 / cellWidth; cx++) {
                probe.advanceCellX(cx);
                for (int cz = 0; cz < 16 / cellWidth; cz++) {
                    probe.selectCellYZ(sampleCellY, cz);
                    probe.updateForY(topWaterY, (double) sampleOffsetY / (double) cellHeight);

                    // One probe at each covered quart center (cellWidth=4 → exact quart centers).
                    for (int qx = 0; qx < 4; qx++) {
                        int localX = qx * 4 + 2;
                        if (localX / cellWidth != cx) {
                            continue;
                        }
                        int blockX = chunkPos.getBlockX(localX);
                        probe.updateForX(blockX, 0.5D);
                        for (int qz = 0; qz < 4; qz++) {
                            int localZ = qz * 4 + 2;
                            if (localZ / cellWidth != cz) {
                                continue;
                            }
                            int blockZ = chunkPos.getBlockZ(localZ);
                            probe.updateForZ(blockZ, 0.5D);
                            BlockState sampledState = probe.getInterpolatedState();
                            if (sampledState == null) {
                                sampledState = defaultBlock;
                            }
                            quartOpenWater[qx * 4 + qz] = sampledState.getFluidState().is(FluidTags.WATER);
                        }
                    }
                    probe.swapSlices();
                }
            }
            probe.stopInterpolation();
        }

        boolean[][] haloParent = new boolean[OceanGenSampler.FIELD_SIZE][OceanGenSampler.FIELD_SIZE];
        for (int qx = 0; qx < OceanGenSampler.FIELD_SIZE; qx++) {
            for (int qz = 0; qz < OceanGenSampler.FIELD_SIZE; qz++) {
                int localX = qx - OceanGenSampler.HALO_QUART_RADIUS;
                int localZ = qz - OceanGenSampler.HALO_QUART_RADIUS;
                if (localX >= 0 && localX < 4 && localZ >= 0 && localZ < 4) {
                    continue;
                }
                ResourceLocation biome = haloBiomeAt.apply(baseQuartX + localX, baseQuartZ + localZ);
                // Same island override as the sampler's own quart cells, so the edge-fade field
                // sees identical support on both sides of a chunk border.
                haloParent[qx][qz] = stack.isParentBiome(biome)
                        || (spawnIsland && com.dexer.aquanaut.common.worldgen.blend.SpawnIslandMask
                                .maskAt((baseQuartX + localX) << 2, (baseQuartZ + localZ) << 2) > 0.0D);
            }
        }

        return new OceanGenSampler(stack, surfaceBiomes, quartOpenWater, haloParent,
                baseQuartX, baseQuartZ, spawnIsland);
    }

    public static int topWaterY(OceanLayerStack stack) {
        TerrainModule terrain = stackTerrain(stack);
        return terrain.topWaterY();
    }

    /**
     * True when {@link #sample} would mark at least one quart column of this chunk as supported,
     * i.e. the fill planned real mod terrain here. Vanilla carvers must never touch covered
     * chunks: the mod carves its own voids analytically, and any carved air pocket inside the
     * drowned terrain culls the surrounding water faces.
     *
     * <p>
     * A cheap parent-biome pre-check runs first (the surface probe sits at block Y 64, above
     * every rewrite band, so the palette there is the untouched noise biome at any pipeline
     * stage) and skips the noise probes entirely for land and coast chunks.
     */
    public static boolean isCovered(ChunkAccess chunk,
                                    OceanLayerStack stack,
                                    int topWaterY,
                                    boolean spawnIsland,
                                    int minCellY,
                                    int cellCountY,
                                    int cellWidth,
                                    int cellHeight,
                                    BlockState defaultBlock,
                                    java.util.function.Supplier<NoiseChunk> probeFactory,
                                    java.util.function.BiFunction<Integer, Integer, ResourceLocation> haloBiomeAt) {
        ChunkPos chunkPos = chunk.getPos();
        int baseQuartX = QuartPos.fromBlock(chunkPos.getMinBlockX());
        int baseQuartZ = QuartPos.fromBlock(chunkPos.getMinBlockZ());
        int surfaceQuartY = MiddleLevelOceanPlacement.surfaceSampleQuartY();
        boolean parentPresent = false;
        for (int lx = 0; lx < OceanGenSampler.QUARTS && !parentPresent; lx++) {
            for (int lz = 0; lz < OceanGenSampler.QUARTS; lz++) {
                ResourceLocation biome = chunk.getNoiseBiome(baseQuartX + lx, surfaceQuartY, baseQuartZ + lz)
                        .unwrapKey()
                        .map(key -> key.location())
                        .orElse(null);
                if (stack.isParentBiome(biome)) {
                    parentPresent = true;
                    break;
                }
            }
        }
        if (!parentPresent) {
            return false;
        }
        return sample(chunk, stack, topWaterY, spawnIsland, minCellY, cellCountY, cellWidth, cellHeight,
                defaultBlock, probeFactory, haloBiomeAt).anySupported();
    }

    public static TerrainModule stackTerrain(OceanLayerStack stack) {
        ResourceLocation terrainId = stack.layers().get(stack.layers().size() - 1).terrain();
        return OceanLayerStacks.terrain(terrainId);
    }
}
