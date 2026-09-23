package com.dexer.aquanaut.common.worldgen.layers;

import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanColumnRules;
import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanPlacement;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

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
                haloParent[qx][qz] = stack.isParentBiome(biome);
            }
        }

        return new OceanGenSampler(stack, surfaceBiomes, quartOpenWater, haloParent, baseQuartX, baseQuartZ);
    }

    public static int topWaterY(OceanLayerStack stack) {
        TerrainModule terrain = stackTerrain(stack);
        return terrain.topWaterY();
    }

    public static TerrainModule stackTerrain(OceanLayerStack stack) {
        ResourceLocation terrainId = stack.layers().get(stack.layers().size() - 1).terrain();
        return OceanLayerStacks.terrain(terrainId);
    }
}
