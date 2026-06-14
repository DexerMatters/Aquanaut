package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.common.worldgen.CoralForestPlacement;
import com.dexer.aquanaut.common.worldgen.JellyJunglePlacement;
import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanColumnRules;
import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanPlacement;
import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanTerrainShaper;
import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanTransitionField;
import com.dexer.aquanaut.common.worldgen.MiddleLevelOceanTransitionSupport;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    private static final int CURRENT_CHUNK_QUART_SIZE = 4;
    private static final int HALO_QUART_RADIUS = 4;
    private static final int TRANSITION_FIELD_SIZE = CURRENT_CHUNK_QUART_SIZE + HALO_QUART_RADIUS * 2;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    @Shadow
    @Final
    private Holder<NoiseGeneratorSettings> settings;

    @Shadow
    protected abstract NoiseChunk createNoiseChunk(ChunkAccess chunk, StructureManager structureManager, Blender blender, RandomState randomState);

    @Inject(method = "doFill",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void aquanaut$integratedFill(Blender blender,
                                         StructureManager structureManager,
                                         RandomState randomState,
                                         ChunkAccess chunk,
                                         int minCellY,
                                         int cellCountY,
                                         CallbackInfoReturnable<ChunkAccess> cir) {
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(
                c -> this.createNoiseChunk(c, structureManager, blender, randomState));
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunk.getPos();
        int minBlockX = chunkPos.getMinBlockX();
        int minBlockZ = chunkPos.getMinBlockZ();
        Aquifer aquifer = noiseChunk.aquifer();
        noiseChunk.initializeForFirstCellX();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int cellWidth = noiseChunk.cellWidth();
        int cellHeight = noiseChunk.cellHeight();
        int cellsX = 16 / cellWidth;
        int cellsZ = 16 / cellWidth;
        BlockState defaultBlock = this.settings.value().defaultBlock();
        QuartSupport quartSupport = aquanaut$computeQuartSupport(chunk,
                structureManager,
                blender,
                randomState,
                minCellY,
                cellCountY,
                cellWidth,
                cellHeight,
                cellsX,
                cellsZ,
                defaultBlock);
        aquanaut$rewriteLowerBiomes(chunk, quartSupport);

        MiddleLevelOceanTerrainShaper.ColumnData[] columns =
                MiddleLevelOceanTerrainShaper.precomputeColumns(chunk, quartSupport.transitionField());

        for (int cx = 0; cx < cellsX; cx++) {
            noiseChunk.advanceCellX(cx);

            for (int cz = 0; cz < cellsZ; cz++) {
                int sectionIdx = chunk.getSectionsCount() - 1;
                LevelChunkSection section = chunk.getSection(sectionIdx);

                for (int cy = cellCountY - 1; cy >= 0; cy--) {
                    noiseChunk.selectCellYZ(cy, cz);

                    for (int ly = cellHeight - 1; ly >= 0; ly--) {
                        int blockY = (minCellY + cy) * cellHeight + ly;
                        int localY = blockY & 15;
                        int newSectionIdx = chunk.getSectionIndex(blockY);
                        if (sectionIdx != newSectionIdx) {
                            sectionIdx = newSectionIdx;
                            section = chunk.getSection(newSectionIdx);
                        }

                        double fracY = (double) ly / (double) cellHeight;
                        noiseChunk.updateForY(blockY, fracY);

                        for (int lx = 0; lx < cellWidth; lx++) {
                            int blockX = minBlockX + cx * cellWidth + lx;
                            int localX = blockX & 15;
                            double fracX = (double) lx / (double) cellWidth;
                            noiseChunk.updateForX(blockX, fracX);

                            for (int lz = 0; lz < cellWidth; lz++) {
                                int blockZ = minBlockZ + cz * cellWidth + lz;
                                int localZ = blockZ & 15;
                                double fracZ = (double) lz / (double) cellWidth;
                                noiseChunk.updateForZ(blockZ, fracZ);

                                BlockState blockstate;
                                MiddleLevelOceanTerrainShaper.ColumnData col = columns[localX * 16 + localZ];

                                if (col != null && blockY <= col.topCarveY() && blockY > col.bottomY()) {
                                    noiseChunk.getInterpolatedState();
                                    blockstate = col.stateForY(blockY);
                                } else {
                                    blockstate = noiseChunk.getInterpolatedState();
                                    if (blockstate == null) {
                                        blockstate = defaultBlock;
                                    }
                                }

                                if (blockstate != AIR && !SharedConstants.debugVoidTerrain(chunkPos)) {
                                    section.setBlockState(localX, localY, localZ, blockstate, false);
                                    oceanFloor.update(localX, blockY, localZ, blockstate);
                                    worldSurface.update(localX, blockY, localZ, blockstate);
                                    if (aquifer.shouldScheduleFluidUpdate() && !blockstate.getFluidState().isEmpty()) {
                                        mutablePos.set(blockX, blockY, blockZ);
                                        chunk.markPosForPostprocessing(mutablePos);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            noiseChunk.swapSlices();
        }

        noiseChunk.stopInterpolation();
        cir.setReturnValue(chunk);
    }

    @Unique
    private QuartSupport aquanaut$computeQuartSupport(ChunkAccess chunk,
                                                      StructureManager structureManager,
                                                      Blender blender,
                                                      RandomState randomState,
                                                      int minCellY,
                                                      int cellCountY,
                                                      int cellWidth,
                                                      int cellHeight,
                                                      int cellsX,
                                                      int cellsZ,
                                                      BlockState defaultBlock) {
        boolean[] currentChunkOpenWaterColumns = new boolean[256];
        int topWaterY = MiddleLevelOceanTerrainShaper.topWaterY();
        int sampleCellY = Math.floorDiv(topWaterY, cellHeight) - minCellY;
        int sampleOffsetY = Math.floorMod(topWaterY, cellHeight);

        if (sampleCellY >= 0 && sampleCellY < cellCountY) {
            NoiseChunk previewNoiseChunk = this.createNoiseChunk(chunk, structureManager, blender, randomState);
            previewNoiseChunk.initializeForFirstCellX();

            for (int cx = 0; cx < cellsX; cx++) {
                previewNoiseChunk.advanceCellX(cx);

                for (int cz = 0; cz < cellsZ; cz++) {
                    previewNoiseChunk.selectCellYZ(sampleCellY, cz);
                    previewNoiseChunk.updateForY(topWaterY, (double) sampleOffsetY / (double) cellHeight);

                    for (int lx = 0; lx < cellWidth; lx++) {
                        int localX = cx * cellWidth + lx;
                        int blockX = chunk.getPos().getBlockX(localX);
                        previewNoiseChunk.updateForX(blockX, (double) lx / (double) cellWidth);

                        for (int lz = 0; lz < cellWidth; lz++) {
                            int localZ = cz * cellWidth + lz;
                            int blockZ = chunk.getPos().getBlockZ(localZ);
                            previewNoiseChunk.updateForZ(blockZ, (double) lz / (double) cellWidth);

                            BlockState sampledState = previewNoiseChunk.getInterpolatedState();
                            if (sampledState == null) {
                                sampledState = defaultBlock;
                            }

                            currentChunkOpenWaterColumns[localX * 16 + localZ] =
                                    sampledState.getFluidState().is(FluidTags.WATER);
                        }
                    }
                }

                previewNoiseChunk.swapSlices();
            }

            previewNoiseChunk.stopInterpolation();
        }

        ResourceLocation[][] surfaceBiomes = new ResourceLocation[CURRENT_CHUNK_QUART_SIZE][CURRENT_CHUNK_QUART_SIZE];
        int[][] openWaterCounts = new int[CURRENT_CHUNK_QUART_SIZE][CURRENT_CHUNK_QUART_SIZE];
        boolean[][] transitionSupport = new boolean[TRANSITION_FIELD_SIZE][TRANSITION_FIELD_SIZE];
        ChunkPos chunkPos = chunk.getPos();
        int baseQuartX = QuartPos.fromBlock(chunkPos.getMinBlockX());
        int baseQuartZ = QuartPos.fromBlock(chunkPos.getMinBlockZ());
        int surfaceQuartY = MiddleLevelOceanPlacement.surfaceSampleQuartY();
        ChunkGeneratorAccessor generatorAccessor = (ChunkGeneratorAccessor) this;

        for (int localX = 0; localX < 16; localX++) {
            int quartLocalX = localX >> 2;
            for (int localZ = 0; localZ < 16; localZ++) {
                if (currentChunkOpenWaterColumns[localX * 16 + localZ]) {
                    openWaterCounts[quartLocalX][localZ >> 2]++;
                }
            }
        }

        for (int qx = 0; qx < TRANSITION_FIELD_SIZE; qx++) {
            for (int qz = 0; qz < TRANSITION_FIELD_SIZE; qz++) {
                int worldQuartX = baseQuartX - HALO_QUART_RADIUS + qx;
                int worldQuartZ = baseQuartZ - HALO_QUART_RADIUS + qz;
                int localQuartX = qx - HALO_QUART_RADIUS;
                int localQuartZ = qz - HALO_QUART_RADIUS;

                ResourceLocation biomeLocation;
                int openWaterCount;
                if (localQuartX >= 0
                        && localQuartX < CURRENT_CHUNK_QUART_SIZE
                        && localQuartZ >= 0
                        && localQuartZ < CURRENT_CHUNK_QUART_SIZE) {
                    biomeLocation = chunk.getNoiseBiome(worldQuartX, surfaceQuartY, worldQuartZ)
                            .unwrapKey()
                            .map(key -> key.location())
                            .orElse(null);
                    openWaterCount = openWaterCounts[localQuartX][localQuartZ];
                    surfaceBiomes[localQuartX][localQuartZ] = biomeLocation;
                    transitionSupport[qx][qz] = MiddleLevelOceanTransitionSupport.supportsCurrentChunkCell(
                            biomeLocation,
                            openWaterCount);
                } else {
                    biomeLocation = generatorAccessor.aquanaut$getBiomeSource()
                            .getNoiseBiome(worldQuartX, surfaceQuartY, worldQuartZ, randomState.sampler())
                            .unwrapKey()
                            .map(key -> key.location())
                            .orElse(null);
                    transitionSupport[qx][qz] = MiddleLevelOceanTransitionSupport.supportsHaloCell(biomeLocation);
                }
            }
        }

        return new QuartSupport(surfaceBiomes,
                openWaterCounts,
                new MiddleLevelOceanTransitionField(transitionSupport, HALO_QUART_RADIUS, HALO_QUART_RADIUS));
    }

    @Unique
    private void aquanaut$rewriteLowerBiomes(ChunkAccess chunk, QuartSupport quartSupport) {
        ChunkGeneratorAccessor generatorAccessor = (ChunkGeneratorAccessor) this;
        Holder<Biome> coralForest = aquanaut$findBiome(generatorAccessor.aquanaut$getBiomeSource(),
                CoralForestPlacement.location());
        Holder<Biome> jellyJungle = aquanaut$findBiome(generatorAccessor.aquanaut$getBiomeSource(),
                JellyJunglePlacement.location());
        Holder<Biome> middleLevelOcean = aquanaut$findBiome(generatorAccessor.aquanaut$getBiomeSource(),
                MiddleLevelOceanPlacement.location());
        if (coralForest == null || jellyJungle == null || middleLevelOcean == null) {
            return;
        }

        int minQuartY = QuartPos.fromBlock(chunk.getMinBuildHeight());
        int maxQuartY = minQuartY + QuartPos.fromBlock(chunk.getHeight()) - 1;
        int coralForestMaxQuartY = Math.min(CoralForestPlacement.layerStartQuartY(), maxQuartY);
        if (coralForestMaxQuartY < minQuartY) {
            return;
        }
        int baseQuartX = QuartPos.fromBlock(chunk.getPos().getMinBlockX());
        int baseQuartZ = QuartPos.fromBlock(chunk.getPos().getMinBlockZ());

        for (int localQuartX = 0; localQuartX < 4; localQuartX++) {
            for (int localQuartZ = 0; localQuartZ < 4; localQuartZ++) {
                ResourceLocation surfaceBiomeLocation = quartSupport.surfaceBiomes()[localQuartX][localQuartZ];
                int openWaterColumns = quartSupport.openWaterCounts()[localQuartX][localQuartZ];
                if (!MiddleLevelOceanColumnRules.supportsQuartCell(surfaceBiomeLocation, openWaterColumns)) {
                    continue;
                }

                for (int quartY = minQuartY; quartY <= coralForestMaxQuartY; quartY++) {
                    Holder<Biome> targetBiome = aquanaut$targetBiomeHolder(
                            MiddleLevelOceanColumnRules.targetBiome(surfaceBiomeLocation,
                                    openWaterColumns,
                                    baseQuartX + localQuartX,
                                    quartY,
                                    baseQuartZ + localQuartZ),
                            coralForest,
                            jellyJungle,
                            middleLevelOcean);
                    if (targetBiome == null) {
                        continue;
                    }

                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(QuartPos.toBlock(quartY)));
                    @SuppressWarnings("unchecked")
                    PalettedContainer<Holder<Biome>> biomes = (PalettedContainer<Holder<Biome>>) section.getBiomes();
                    biomes.set(localQuartX, Math.floorMod(quartY, 4), localQuartZ, targetBiome);
                }
            }
        }
    }

    @Unique
    private static Holder<Biome> aquanaut$findBiome(net.minecraft.world.level.biome.BiomeSource biomeSource,
                                                    ResourceLocation biomeLocation) {
        for (Holder<Biome> biome : biomeSource.possibleBiomes()) {
            if (biome.is(biomeLocation)) {
                return biome;
            }
        }

        return null;
    }

    @Unique
    private static Holder<Biome> aquanaut$targetBiomeHolder(MiddleLevelOceanColumnRules.TargetBiome targetBiome,
                                                            Holder<Biome> coralForest,
                                                            Holder<Biome> jellyJungle,
                                                            Holder<Biome> middleLevelOcean) {
        return switch (targetBiome) {
            case CORAL_FOREST -> coralForest;
            case JELLY_JUNGLE -> jellyJungle;
            case MIDDLE_LEVEL_OCEAN -> middleLevelOcean;
            case NONE -> null;
        };
    }

    @Unique
    private record QuartSupport(ResourceLocation[][] surfaceBiomes,
                                int[][] openWaterCounts,
                                MiddleLevelOceanTransitionField transitionField) {
    }
}
